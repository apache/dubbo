/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.rpc.cluster.loadbalance;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * KKEY 经过多次重构，目前版本2.7.2的算法。平滑加权轮询负载算法，通过了各路大神重构之后的算法。
 * 每个服务器对应两个权重，分别为 weight 和 currentWeight。
 * 其中 weight 是固定的，currentWeight 会动态调整，初始值为0。
 * 当有新的请求进来时，遍历服务器列表，让它的 currentWeight 加上自身权重。
 * 遍历完成后，找到最大的 currentWeight，并将其减去权重总和，然后获取currentWeight最大的Invoker返回。
 *
 * 这里仍然使用服务器 [A, B, C] 对应权重 [5, 1, 1] 的例子说明，现在有7个请求依次进入负载均衡逻辑，选择过程如下：
 *
 * [请求编号]	[currentWeight数组]	[减去权重总和后的currentWeight数组] [加上weight之后的currentWeight数组用于下一次计算]
 * 1	    [5, 1, 1]	          [-2, 1, 1]                     [3, 2, 2]
 * 2	    [3, 2, 2]	          [-4, 2, 2]                     [1, 3, 3]
 * 3	    [1, 3, 3]	          [1, -4, 3]                     [6, -3, 4]
 * 4	    [6, -3, 4]	          [-1, -3, 4]                    [4, -2, 5]
 * 5	    [4, -2, 5]	          [4, -2, -2]                    [9, -1, -1]
 * 6	    [9, -1, -1]	          [2, -1, -1]                    [7, 0, 0]
 * 7	    [7, 0, 0]	          [0, 0, 0]                      [5, 1, 1]一个轮回！
 * 如上，经过平滑性处理后，得到的服务器序列为 [A, A, B, A, C, A, A]，相比之前的序列 [A, A, A, A, A, B, C]，分布性要好一些。
 * 初始情况下 currentWeight = [0, 0, 0]，第7个请求处理完后，currentWeight 再次变为 [0, 0, 0]。
 *
 *
 */
public class RoundRobinLoadBalance extends AbstractLoadBalance {
    public static final String NAME = "roundrobin";

    private static final int RECYCLE_PERIOD = 60000;//回收间隔
    // 嵌套 Map 结构，存储的数据结构示例如下：每个方法的权重
    // {
    //     "UserService.query": {
    //         "url1": WeightedRoundRobin@123,
    //         "url2": WeightedRoundRobin@456,
    //     },
    //     "UserService.update": {
    //         "url1": WeightedRoundRobin@123,
    //         "url2": WeightedRoundRobin@456,
    //     }
    // }
    // 最外层为服务类名 + 方法名，第二层为 url 到 WeightedRoundRobin 的映射关系。
    // 这里我们可以将 url 看成是服务提供者的 id
    private ConcurrentMap<String, ConcurrentMap<String, WeightedRoundRobin>> methodWeightMap = new ConcurrentHashMap<String, ConcurrentMap<String, WeightedRoundRobin>>();
    private AtomicBoolean updateLock = new AtomicBoolean();

    protected <T> Collection<String> getInvokerAddrList(List<Invoker<T>> invokers, Invocation invocation) {
        String key = invokers.get(0).getUrl().getServiceKey() + "." + invocation.getMethodName();
        Map<String, WeightedRoundRobin> map = methodWeightMap.get(key);
        if (map != null) {
            return map.keySet();
        }
        return null;
    }

    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        // key = 全限定类名 + "." + 方法名，比如 com.xxx.DemoService.sayHello
        String key = invokers.get(0).getUrl().getServiceKey() + "." + invocation.getMethodName();
        ConcurrentMap<String, WeightedRoundRobin> map = methodWeightMap.get(key);
        if (map == null) {
            methodWeightMap.putIfAbsent(key, new ConcurrentHashMap<>());
            map = methodWeightMap.get(key);
        }
        int totalWeight = 0;
        long maxCurrent = Long.MIN_VALUE;
        long now = System.currentTimeMillis();
        Invoker<T> selectedInvoker = null;
        WeightedRoundRobin selectedWRR = null;
        for (Invoker<T> invoker : invokers) {//遍历列表
            String identifyString = invoker.getUrl().toIdentityString();//invoker标识ID
            WeightedRoundRobin weightedRoundRobin = map.get(identifyString);//获取权重等信息
            int weight = getWeight(invoker, invocation);//重新计算权重

            if (weightedRoundRobin == null) {//权重信息还没初始化则进行初始化
                weightedRoundRobin = new WeightedRoundRobin();
                weightedRoundRobin.setWeight(weight);
                map.putIfAbsent(identifyString, weightedRoundRobin);
            }
            if (weight != weightedRoundRobin.getWeight()) {
                weightedRoundRobin.setWeight(weight);//权重有变动更新权重信息，并设置计算权重为0
            }
            long cur = weightedRoundRobin.increaseCurrent();//权重计算：current计算权重+配置权重
            weightedRoundRobin.setLastUpdate(now);//更新时间
            if (cur > maxCurrent) {//KKEY 找最大的计算后的权重Invoker，记录数据
                maxCurrent = cur;
                selectedInvoker = invoker;
                selectedWRR = weightedRoundRobin;
            }
            totalWeight += weight;//权重累加
        }

        // 对 <identifyString, WeightedRoundRobin> 进行检查，过滤掉长时间未被更新的节点。
        // 该节点可能挂了，invokers 中不包含该节点，所以该节点的 lastUpdate 长时间无法被更新。
        // 若未更新时长超过阈值后，就会被移除掉，默认阈值为60秒。
        if (!updateLock.get() && invokers.size() != map.size()) {
            if (updateLock.compareAndSet(false, true)) {
                try {
                    // 拷贝
                    ConcurrentMap<String, WeightedRoundRobin> newMap = new ConcurrentHashMap<>(map);
                    // 遍历修改，即移除过期记录
                    newMap.entrySet().removeIf(item -> now - item.getValue().getLastUpdate() > RECYCLE_PERIOD);
                    methodWeightMap.put(key, newMap);// 更新引用
                } finally {
                    updateLock.set(false);
                }
            }
        }
        if (selectedInvoker != null) {
            selectedWRR.sel(totalWeight);//如果已经找到计算后最大权重的Invoker，修改其计算的权重=当前权重Current-总权重
            return selectedInvoker;
        }
        // should not happen here
        return invokers.get(0);
    }

    protected static class WeightedRoundRobin {
        private int weight;//服务提供者配置权重
        private AtomicLong current = new AtomicLong(0);//当前计算过的权重
        private long lastUpdate;//上次更新时间戳

        public int getWeight() {
            return weight;
        }

        public void setWeight(int weight) {
            this.weight = weight;
            current.set(0);
        }

        public long increaseCurrent() {
            return current.addAndGet(weight);
        }

        public void sel(int total) {
            current.addAndGet(-1 * total);
        }

        public long getLastUpdate() {
            return lastUpdate;
        }

        public void setLastUpdate(long lastUpdate) {
            this.lastUpdate = lastUpdate;
        }
    }
}
