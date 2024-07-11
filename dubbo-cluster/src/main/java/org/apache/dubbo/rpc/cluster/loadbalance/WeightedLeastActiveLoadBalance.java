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
import org.apache.dubbo.rpc.support.RpcUtils;

import java.security.SecureRandom;
import java.util.List;

/**
 * WeightedLeastActiveLoadBalance
 * <p>
 * Filter the invokers with the least number of active calls based on weight. If there is only one invoker, use the
 * invoker directly; if there are multiple invokers and the leastActive value is 0, then random according to the total
 * weight; if there are multiple invokers and have the same weighted active value, then randomly called.
 */
public class WeightedLeastActiveLoadBalance extends AbstractLoadBalance {

    public static final String NAME = "weightedleastactive";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        // Number of invokers
        int length = invokers.size();
        // The least active value of all invokers
        int leastActive = -1;
        // The number of invokers having the same least active value (leastActive)
        int leastCount = 0;
        // The index of invokers having the same least active value (leastActive)
        int[] leastIndexes = new int[length];
        // the weight of every invoker
        int[] weights = new int[length];
        // The sum of the warmup weights of all the least active invokers
        int totalWeight = 0;

        // Filter out all the least active invokers
        for (int i = 0; i < length; i++) {
            Invoker<T> invoker = invokers.get(i);
            // Get the weight of the invoker's configuration. The default value is 100.
            int afterWarmup = getWeight(invoker, invocation);
            // save for later use
            weights[i] = afterWarmup;
            // Get the weighted active number of the invoker
            int weightedActive = afterWarmup
                    * RpcStatus.getStatus(invoker.getUrl(), RpcUtils.getMethodName(invocation))
                            .getActive();
            // If it is the first invoker or the weightedActive number of the invoker is less than the current least
            // active number
            if (leastActive == -1 || weightedActive < leastActive) {
                // Reset the weightedActive number of the current invoker to the least active number
                leastActive = weightedActive;
                // Reset the number of least active invokers
                leastCount = 1;
                // Put the first least active invoker first in leastIndexes
                leastIndexes[0] = i;
                // Reset totalWeight
                totalWeight = afterWarmup;

                // If current invoker's weightedActive value equals with leaseActive, then accumulating.
            } else if (weightedActive == leastActive) {
                // Record the index of the least active invoker in leastIndexes order
                leastIndexes[leastCount++] = i;
                // Reset totalWeight
                totalWeight += afterWarmup;
            }
        }
        // Choose an invoker from all the least active invokers
        if (leastCount == 1) {
            // If we got exactly one invoker having the least active value, return this invoker directly.
            return invokers.get(leastIndexes[0]);
        }
        // If there are more than one invoker and the leastActive value is 0, pick an invoker by weighted random.
        if (leastActive == 0) {

            int offsetWeight = SECURE_RANDOM.nextInt(totalWeight);
            for (int i = 0; i < leastCount; i++) {
                int leastIndex = leastIndexes[i];
                offsetWeight -= weights[leastIndex];
                if (offsetWeight < 0) {
                    return invokers.get(leastIndex);
                }
            }
        }
        // return evenly.
        return invokers.get(leastIndexes[SECURE_RANDOM.nextInt(leastCount)]);
    }
}
