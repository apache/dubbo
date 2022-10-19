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
import org.apache.dubbo.rpc.AdaptiveMetrics;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdaptiveLoadBalanceTest extends LoadBalanceBaseTest {

    @Test
    @Order(0)
    public void testSelectByWeight() {
        int sumInvoker1 = 0;
        int sumInvoker2 = 0;
        int sumInvoker3 = 0;
        int loop = 10000;

        AdaptiveLoadBalance lb = new AdaptiveLoadBalance();
        for (int i = 0; i < loop; i++) {
            Invoker selected = lb.select(weightInvokers, null, weightTestInvocation);

            if (selected.getUrl().getProtocol().equals("test1")) {
                sumInvoker1++;
            }

            if (selected.getUrl().getProtocol().equals("test2")) {
                sumInvoker2++;
            }

            if (selected.getUrl().getProtocol().equals("test3")) {
                sumInvoker3++;
            }
        }

        // 1 : 9 : 6
        System.out.println(sumInvoker1);
        System.out.println(sumInvoker2);
        System.out.println(sumInvoker3);
        Assertions.assertEquals(sumInvoker1 + sumInvoker2 + sumInvoker3, loop, "select failed!");
    }

    private String buildServiceKey(Invoker invoker){
        URL url = invoker.getUrl();
        return url.getAddress() + ":" + invocation.getProtocolServiceKey();
    }

    @Test
    @Order(1)
    public void testSelectByAdaptive() throws NoSuchFieldException, IllegalAccessException {
        int sumInvoker1 = 0;
        int sumInvoker2 = 0;
        int sumInvoker5 = 0;
        int loop = 10000;

        AdaptiveLoadBalance lb = new AdaptiveLoadBalance();
        lb.setApplicationModel(ApplicationModel.defaultModel());

        lb.select(weightInvokersSR, null, weightTestInvocation);

        for (int i = 0; i < loop; i++) {
            Invoker selected = lb.select(weightInvokersSR, null, weightTestInvocation);

            Map<String, String> metricsMap = new HashMap<>();
            String idKey = buildServiceKey(selected);

            if (selected.getUrl().getProtocol().equals("test1")) {
                sumInvoker1++;
                metricsMap.put("rt", "10");
                metricsMap.put("load", "10");
                metricsMap.put("curTime", String.valueOf(System.currentTimeMillis()-10));
                AdaptiveMetrics.addConsumerSuccess(idKey);
            }

            if (selected.getUrl().getProtocol().equals("test2")) {
                sumInvoker2++;
                metricsMap.put("rt", "100");
                metricsMap.put("load", "40");
                metricsMap.put("curTime", String.valueOf(System.currentTimeMillis()-100));
                AdaptiveMetrics.addConsumerSuccess(idKey);
            }

            if (selected.getUrl().getProtocol().equals("test5")) {
                metricsMap.put("rt", "5000");
                metricsMap.put("load", "400");//400%
                metricsMap.put("curTime", String.valueOf(System.currentTimeMillis() - 5000));

                AdaptiveMetrics.addErrorReq(idKey);
                sumInvoker5++;
            }
            AdaptiveMetrics.setProviderMetrics(idKey,metricsMap);
            try {
                Thread.sleep(3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Map<Invoker<LoadBalanceBaseTest>, Integer> weightMap = weightInvokersSR.stream()
            .collect(Collectors.toMap(Function.identity(), e -> Integer.valueOf(e.getUrl().getParameter("weight"))));
        Integer totalWeight = weightMap.values().stream().reduce(0, Integer::sum);
        // max deviation = expectWeightValue * 2
        int expectWeightValue = loop / totalWeight;
        int maxDeviation = expectWeightValue * 2;
        double beta = 0.5;
        //这个估算值并不准确
        double ewma1 = beta * 50 + (1 - beta) * 10;
        double ewma2 = beta * 50 + (1 - beta) * 100;
        double ewma5 = beta * 50 + (1 - beta) * 5000;

        AtomicInteger weight1 = new AtomicInteger();
        AtomicInteger weight2 = new AtomicInteger();
        AtomicInteger weight5 = new AtomicInteger();
        weightMap.forEach((k, v) ->{
            if (k.getUrl().getProtocol().equals("test1")){
                weight1.set(v);
            }
            else if (k.getUrl().getProtocol().equals("test2")){
                weight2.set(v);
            }
            else if (k.getUrl().getProtocol().equals("test5")){
                weight5.set(v);
            }
        });

        System.out.println(sumInvoker1);
        System.out.println(sumInvoker2);
        System.out.println(sumInvoker5);

        Assertions.assertEquals(sumInvoker1 + sumInvoker2 + sumInvoker5, loop, "select failed!");
        Assertions.assertTrue(Math.abs(sumInvoker1 / (weightMap.get(weightInvoker1) * ewma1) - expectWeightValue) < maxDeviation, "select failed!");
        Assertions.assertTrue(Math.abs(sumInvoker2 / (weightMap.get(weightInvoker2) * ewma2) - expectWeightValue) < maxDeviation, "select failed!");
        Assertions.assertTrue(Math.abs(sumInvoker5 / (weightMap.get(weightInvoker5) * ewma5) - expectWeightValue) < maxDeviation, "select failed!");
    }
}
