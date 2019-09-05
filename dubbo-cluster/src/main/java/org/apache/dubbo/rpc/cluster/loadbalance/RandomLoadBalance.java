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

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** KKEY 引用自Apache-dubbo官方：
 * * RandomLoadBalance 是加权随机算法的具体实现，它的算法思想很简单。
 * * 假设我们有一组服务器 servers = [A, B, C]，他们对应的权重为weights = [5, 3, 2]，权重总和为10。
 * * 现在把这些权重值平铺在一维坐标值上，[0, 5) 区间属于服务器 A，[5, 8) 区间属于服务器 B，[8, 10) 区间属于服务器 C。
 * * 接下来通过随机数生成器生成一个范围在 [0, 10) 之间的随机数，然后计算这个随机数会落到哪个区间上。
 * * 比如数字3会落到服务器 A 对应的区间上，此时返回服务器 A 即可。
 * * 权重越大的机器，在坐标轴上对应的区间范围就越大，因此随机数生成器生成的数字就会有更大的概率落到此区间内。
 * * 只要随机数生成器产生的随机数分布性很好，在经过多次选择后，每个服务器被选中的次数比例接近其权重比例。
 * * 比如，经过一万次选择后，服务器 A 被选中的次数大约为5000次，服务器 B 被选中的次数约为3000次，服务器 C 被选中的次数约为2000次。
 * * 以上就是 RandomLoadBalance 背后的算法思想。 */
public class RandomLoadBalance extends AbstractLoadBalance {
    public static final String NAME = "random";
    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        int length = invokers.size();//服务提供者个数
        boolean sameWeight = true;//是否相同权重，不进行权重区分
        int[] weights = new int[length];
        int firstWeight = getWeight(invokers.get(0), invocation);//获取第一个服务提供者的权重
        weights[0] = firstWeight;
        int totalWeight = firstWeight;//第一个权重初始化总权重，之后依次累加
        //KKEY 以下循环作用，1：统计所有服务提供者的总权重；2、判断所有提供者的权重是否相同；
        for (int i = 1; i < length; i++) {
            int weight = getWeight(invokers.get(i), invocation);//依次获取第二个、第三个。。。服务提供者权重
            weights[i] = weight;
            totalWeight += weight;//权重累加
            if (sameWeight && weight != firstWeight) {//判断各个服务提供者的权重是否相同
                sameWeight = false;
            }
        }
        //KKEY 如果总权重>0 且 各提供者的权重不相同，执行加权随机逻辑，权重都一样就随机一个就可以了；
        if (totalWeight > 0 && !sameWeight) {
            int offset = ThreadLocalRandom.current().nextInt(totalWeight);//在总权重范围内产生一个随机数
            /**
             * KKEY 判断随机数落在哪个权重区间
             * 举例说明一下 servers = [A, B, C]，weights = [5, 3, 2]，随机数：offset = 7。
             * 第一次循环，offset - 5 = 2 > 0，即 offset > 5
             * 表明其不会落在服务器 A 对应的区间上。
             * 第二次循环，此时offset = 2， offset - 3 = -1 < 0，即 5 < offset < 8，
             * 表明其会落在服务器 B 对应的区间上
             */
            for (int i = 0; i < length; i++) {
                offset -= weights[i];
                if (offset < 0) {
                    return invokers.get(i);
                }
            }
        }
        // 如果所有服务提供者权重值相同，此时直接随机返回一个即可
        return invokers.get(ThreadLocalRandom.current().nextInt(length));
    }
}
