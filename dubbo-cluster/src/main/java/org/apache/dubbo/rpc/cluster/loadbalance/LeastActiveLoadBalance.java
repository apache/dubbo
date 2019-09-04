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
import org.apache.dubbo.rpc.RpcStatus;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * kkey 引自：Apache-dubbo官方：
 * LeastActiveLoadBalance 翻译过来是最小活跃数负载均衡。
 * 活跃调用数越小，表明该服务提供者效率越高，单位时间内可处理更多的请求。此时应优先将请求分配给该服务提供者。
 * 在具体实现中，每个服务提供者对应一个活跃数 active。初始情况下，所有服务提供者活跃数均为0。
 * 每收到一个请求，活跃数加1，完成请求后则将活跃数减1。在服务运行一段时间后，性能好的服务提供者处理请求的速度更快，
 * 因此活跃数下降的也越快，此时这样的服务提供者能够优先获取到新的服务请求、这就是最小活跃数负载均衡算法的基本思想。
 * 除了最小活跃数，LeastActiveLoadBalance 在实现上还引入了 kkey 权重值。
 * 所以准确的来说，LeastActiveLoadBalance 是基于加权最小活跃数算法实现的。
 * 举个例子说明一下，在一个服务提供者集群中，有两个性能优异的服务提供者。某一时刻它们的活跃数相同，
 * 此时 Dubbo 会根据它们的权重去分配请求，权重越大，获取到新请求的概率就越大。如果两个服务提供者权重相同，此时随机选择一个即可
 */
public class LeastActiveLoadBalance extends AbstractLoadBalance {

    public static final String NAME = "leastactive";

    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        int length = invokers.size();//服务提供者列表
        int leastActive = -1;//最小活跃数，记录所有invoker中最小的活跃数
        int leastCount = 0;//具有相同最小活跃数的invoke数量，例如invoker列表中最小活跃数等于1的有第三个和第五个，此时数量为2个。

        //记录最小活跃数invoker在invoker列表中的下表，例如最小活跃数等于1的有invoker列表的第三个和第五个invoker
        //则leastIndexes记录的是leastIndexes[0]=3,leastIndexes[1]=5，最后从第三个和第五个中加权随机一个。
        int[] leastIndexes = new int[length];

        int[] weights = new int[length];//记录invoker列表中，每一个invoker的权重；

        //总权重，记录最小活跃数相同的invoker的权重之和，例如上面例子的第三个和第五个最小活跃数都为1，此时第三个的权重是30，
        //第五个的权重是50，则累加为80，到时候加权随机的时候，从80里随机判断落在哪个区间；
        int totalWeight = 0;

        //第一个权重，记录最小活跃数的权重，当发现最小活跃数相同的invoker之后，判断是否权重一样
        //例如上面例子的第三个和第五个最小活跃数都为1，此时第三个的权重是30，轮训到第五个的时候，权重是50;
        // 此时30!=50，置sameWeight=false，如果第五个的权重也为30，则 sameWeight = true
        int firstWeight = 0;
        boolean sameWeight = true;//权重是否都一样的标志

        for (int i = 0; i < length; i++) {
            Invoker<T> invoker = invokers.get(i);//遍历每个invoker
            //获取当前服务提供者的活跃数
            int active = RpcStatus.getStatus(invoker.getUrl(), invocation.getMethodName()).getActive();
            int afterWarmup = getWeight(invoker, invocation);//获取权重
            weights[i] = afterWarmup;//记录权重
            if (leastActive == -1 || active < leastActive) {//发现比最小活跃数更小的invoker
                leastActive = active;//将当前invoker的活跃数记录成最小的活跃数
                leastCount = 1;//相同数量的最小活跃数invoker是设置成1
                leastIndexes[0] = i;//记录当前最小活跃数的invoker的下表到leastIndexes数组的第一个元素中
                totalWeight = afterWarmup;//因为已经找到最小活跃数了，重置相同最小活跃数的权重累加值为当前已经找到的最小活跃数的invoker的权重
                firstWeight = afterWarmup;//且记录当前最小活跃数invoker的权重，用于后面判断相同最小活跃数的invoker的权重是否一样
                sameWeight = true;
            } else if (active == leastActive) {//当前invoker的活跃数和最小活跃数相同
                leastIndexes[leastCount++] = i;//记录下当前invoker的小标到leastIndexes数组中
                totalWeight += afterWarmup;//权重累加
                // 检测当前 Invoker 的权重与 firstWeight 是否相等，不相等则将 sameWeight 置为 false
                if (sameWeight && i > 0 && afterWarmup != firstWeight) {
                    sameWeight = false;
                }
            }
        }
        if (leastCount == 1) {
            //如果相同最小活跃数的invoker数量为1，则下表记录在leastIndexes数组的第一个元素中，直接取则可
            return invokers.get(leastIndexes[0]);
        }
        if (!sameWeight && totalWeight > 0) {
            //多个相同活跃数的invoker权重不相同，且这些invoker的累计权重>0，则在累计权重范围内双随机一个数；
            int offsetWeight = ThreadLocalRandom.current().nextInt(totalWeight);
            for (int i = 0; i < leastCount; i++) {
                int leastIndex = leastIndexes[i];
                offsetWeight -= weights[leastIndex];
                if (offsetWeight < 0) {//判断随机数落在哪个invoker权重的区间，则就是这个了
                    return invokers.get(leastIndex);
                }
            }
        }
        // 如果权重相同或权重为0时，随机返回一个Invoker就好了
        return invokers.get(leastIndexes[ThreadLocalRandom.current().nextInt(leastCount)]);
    }
}