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

import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class PowerOfTwoChoiceWithLeastActiveTest extends LoadBalanceBaseTest{
    @Test
    public void testP2CSelect(){
        int run = 1000;

        for(int i = 0;i < 5;i++){
            for(int j = 0;j <= i;j++){
                RpcStatus.beginCount(invokers.get(i).getUrl(), invocation.getMethodName());
            }
        }

        Map<Invoker, AtomicLong> counter = getInvokeCounter(run,PowerOfTwoChoiceWithLeastActive.NAME);
        for(int i=1;i<5;i++){
            Long count1 = counter.get(invokers.get(i-1)).get();
            Long count2 = counter.get(invokers.get(i)).get();
            Assertions.assertTrue(count1 > count2);
        }
    }
}
