/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License")); you may not use this file except in compliance with
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

import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcStatus;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ShortestResponseLoadBalanceTest extends LoadBalanceBaseTest {

    @Test
    @Order(0)
    public void testSelectByWeight() {
        int sumInvoker1 = 0;
        int sumInvoker2 = 0;
        int loop = 10000;

        ShortestResponseLoadBalance lb = new ShortestResponseLoadBalance();
        lb.setApplicationModel(ApplicationModel.defaultModel());
        for (int i = 0; i < loop; i++) {
            Invoker selected = lb.select(weightInvokersSR, null, weightTestInvocation);

            if (selected.getUrl().getProtocol().equals("test1")) {
                sumInvoker1++;
            }

            if (selected.getUrl().getProtocol().equals("test2")) {
                sumInvoker2++;
            }
            // never select invoker5 because it's estimated response time is more than invoker1 and invoker2
            Assertions.assertTrue(!selected.getUrl().getProtocol().equals("test5"), "select is not the shortest one");
        }

        // the sumInvoker1 : sumInvoker2 approximately equal to 1: 9
        System.out.println(sumInvoker1);
        System.out.println(sumInvoker2);

        Assertions.assertEquals(sumInvoker1 + sumInvoker2, loop, "select failed!");
    }

    @Test
    @Order(1)
    public void testSelectByResponse() throws NoSuchFieldException, IllegalAccessException {
        int sumInvoker1 = 0;
        int sumInvoker2 = 0;
        int sumInvoker5 = 0;
        int loop = 10000;

        //active -> 0
        RpcStatus.endCount(weightInvoker5.getUrl(), weightTestInvocation.getMethodName(), 5000L, true);
        ShortestResponseLoadBalance lb = new ShortestResponseLoadBalance();
        lb.setApplicationModel(ApplicationModel.defaultModel());

        //reset slideWindow
        Field lastUpdateTimeField = ReflectUtils.forName(ShortestResponseLoadBalance.class.getName()).getDeclaredField("lastUpdateTime");
        lastUpdateTimeField.setAccessible(true);
        lastUpdateTimeField.setLong(lb, System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(31));
        lb.select(weightInvokersSR, null, weightTestInvocation);

        for (int i = 0; i < loop; i++) {
            Invoker selected = lb.select(weightInvokersSR, null, weightTestInvocation);

            if (selected.getUrl().getProtocol().equals("test1")) {
                sumInvoker1++;
            }

            if (selected.getUrl().getProtocol().equals("test2")) {
                sumInvoker2++;
            }

            if (selected.getUrl().getProtocol().equals("test5")) {
                sumInvoker5++;
            }
        }
        Map<Invoker<LoadBalanceBaseTest>, Integer> weightMap = weightInvokersSR.stream()
            .collect(Collectors.toMap(Function.identity(), e -> Integer.valueOf(e.getUrl().getParameter("weight"))));
        Integer totalWeight = weightMap.values().stream().reduce(0, Integer::sum);
        // max deviation = expectWeightValue * 2
        int expectWeightValue = loop / totalWeight;
        int maxDeviation = expectWeightValue * 2;

        Assertions.assertEquals(sumInvoker1 + sumInvoker2 + sumInvoker5, loop, "select failed!");
        Assertions.assertTrue(Math.abs(sumInvoker2 / weightMap.get(weightInvoker2) - expectWeightValue) < maxDeviation, "select failed!");
        Assertions.assertTrue(Math.abs(sumInvoker5 / weightMap.get(weightInvoker5) - expectWeightValue) < maxDeviation, "select failed!");
    }
}
