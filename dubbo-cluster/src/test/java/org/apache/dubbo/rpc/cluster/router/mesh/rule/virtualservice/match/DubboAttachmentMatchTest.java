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

package org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.match;


import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.router.mesh.util.TracingContextProvider;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class DubboAttachmentMatchTest {

    @Test
    void dubboContextMatch() {
        DubboAttachmentMatch dubboAttachmentMatch = new DubboAttachmentMatch();

        Map<String, StringMatch> dubbocontextMatchMap = new HashMap<>();

        StringMatch nameMatch = new StringMatch();
        nameMatch.setExact("qinliujie");
        dubbocontextMatchMap.put("name", nameMatch);


        StringMatch machineGroupMatch = new StringMatch();
        machineGroupMatch.setExact("test_host");
        dubbocontextMatchMap.put("machineGroup", machineGroupMatch);

        dubboAttachmentMatch.setDubboContext(dubbocontextMatchMap);

        Map<String, String> invokeDubboContextMap = new HashMap<>();
        invokeDubboContextMap.put("name", "qinliujie");
        invokeDubboContextMap.put("machineGroup", "test_host");
        invokeDubboContextMap.put("other", "other");

        RpcInvocation rpcInvocation = new RpcInvocation();
        rpcInvocation.setAttachments(invokeDubboContextMap);

        assertTrue(dubboAttachmentMatch.isMatch(rpcInvocation, Collections.emptySet()));


        Map<String, String> invokeDubboContextMap2 = new HashMap<>();
        invokeDubboContextMap2.put("name", "jack");
        invokeDubboContextMap2.put("machineGroup", "test_host");
        invokeDubboContextMap2.put("other", "other");

        RpcInvocation rpcInvocation2 = new RpcInvocation();
        rpcInvocation2.setAttachments(invokeDubboContextMap2);

        assertFalse(dubboAttachmentMatch.isMatch(rpcInvocation2, Collections.emptySet()));


        Map<String, String> invokeDubboContextMap3 = new HashMap<>();
        invokeDubboContextMap3.put("name", "qinliujie");
        invokeDubboContextMap3.put("machineGroup", "my_host");
        invokeDubboContextMap3.put("other", "other");

        RpcInvocation rpcInvocation3 = new RpcInvocation();
        rpcInvocation3.setAttachments(invokeDubboContextMap3);

        assertFalse(dubboAttachmentMatch.isMatch(rpcInvocation3, Collections.emptySet()));
    }


    @Test
    void tracingContextMatch() {
        DubboAttachmentMatch dubboAttachmentMatch = new DubboAttachmentMatch();

        Map<String, StringMatch> tracingContextMatchMap = new HashMap<>();

        StringMatch nameMatch = new StringMatch();
        nameMatch.setExact("qinliujie");
        tracingContextMatchMap.put("name", nameMatch);

        StringMatch machineGroupMatch = new StringMatch();
        machineGroupMatch.setExact("test_host");
        tracingContextMatchMap.put("machineGroup", machineGroupMatch);

        dubboAttachmentMatch.setTracingContext(tracingContextMatchMap);

        Map<String, String> invokeEagleEyeContextMap = new HashMap<>();
        invokeEagleEyeContextMap.put("name", "qinliujie");
        invokeEagleEyeContextMap.put("machineGroup", "test_host");
        invokeEagleEyeContextMap.put("other", "other");

        TracingContextProvider tracingContextProvider = (invocation, key) -> invokeEagleEyeContextMap.get(key);
        assertTrue(dubboAttachmentMatch.isMatch(Mockito.mock(Invocation.class), Collections.singleton(tracingContextProvider)));

        Map<String, String> invokeTracingContextMap2 = new HashMap<>();
        invokeTracingContextMap2.put("name", "jack");
        invokeTracingContextMap2.put("machineGroup", "test_host");
        invokeTracingContextMap2.put("other", "other");

        TracingContextProvider tracingContextProvider2 = (invocation, key) -> invokeTracingContextMap2.get(key);
        assertFalse(dubboAttachmentMatch.isMatch(Mockito.mock(Invocation.class), Collections.singleton(tracingContextProvider2)));


        Map<String, String> invokeEagleEyeContextMap3 = new HashMap<>();
        invokeEagleEyeContextMap3.put("name", "qinliujie");
        invokeEagleEyeContextMap3.put("machineGroup", "my_host");
        invokeEagleEyeContextMap3.put("other", "other");

        TracingContextProvider tracingContextProvider3 = (invocation, key) -> invokeEagleEyeContextMap3.get(key);
        assertFalse(dubboAttachmentMatch.isMatch(Mockito.mock(Invocation.class), Collections.singleton(tracingContextProvider3)));
    }


    @Test
    void contextMatch() {
        DubboAttachmentMatch dubboAttachmentMatch = new DubboAttachmentMatch();

        Map<String, StringMatch> tracingContextMatchMap = new HashMap<>();
        StringMatch nameMatch = new StringMatch();
        nameMatch.setExact("qinliujie");
        tracingContextMatchMap.put("name", nameMatch);
        dubboAttachmentMatch.setTracingContext(tracingContextMatchMap);

        Map<String, String> invokeTracingContextMap = new HashMap<>();
        invokeTracingContextMap.put("name", "qinliujie");
        invokeTracingContextMap.put("machineGroup", "test_host");
        invokeTracingContextMap.put("other", "other");

        Map<String, StringMatch> dubboContextMatchMap = new HashMap<>();
        StringMatch dpathMatch = new StringMatch();
        dpathMatch.setExact("PRE");
        dubboContextMatchMap.put("dpath", dpathMatch);
        dubboAttachmentMatch.setDubboContext(dubboContextMatchMap);

        Map<String, String> invokeDubboContextMap = new HashMap<>();
        invokeDubboContextMap.put("dpath", "PRE");

        TracingContextProvider tracingContextProvider = (invocation, key) -> invokeTracingContextMap.get(key);
        RpcInvocation rpcInvocation = new RpcInvocation();
        rpcInvocation.setAttachments(invokeDubboContextMap);
        assertTrue(dubboAttachmentMatch.isMatch(rpcInvocation, Collections.singleton(tracingContextProvider)));


        Map<String, String> invokeTracingContextMap1 = new HashMap<>();
        invokeTracingContextMap1.put("name", "jack");
        invokeTracingContextMap1.put("machineGroup", "test_host");
        invokeTracingContextMap1.put("other", "other");

        TracingContextProvider tracingContextProvider1 = (invocation, key) -> invokeTracingContextMap1.get(key);
        RpcInvocation rpcInvocation1 = new RpcInvocation();
        rpcInvocation1.setAttachments(invokeDubboContextMap);
        assertFalse(dubboAttachmentMatch.isMatch(rpcInvocation1, Collections.singleton(tracingContextProvider1)));


        Map<String, String> invokeDubboContextMap1 = new HashMap<>();
        invokeDubboContextMap1.put("dpath", "PRE-2");

        TracingContextProvider tracingContextProvider2 = (invocation, key) -> invokeTracingContextMap.get(key);
        RpcInvocation rpcInvocation2 = new RpcInvocation();
        rpcInvocation2.setAttachments(invokeDubboContextMap1);
        assertFalse(dubboAttachmentMatch.isMatch(rpcInvocation2, Collections.singleton(tracingContextProvider2)));


        TracingContextProvider tracingContextProvider3 = (invocation, key) -> invokeTracingContextMap1.get(key);
        RpcInvocation rpcInvocation3 = new RpcInvocation();
        rpcInvocation3.setAttachments(invokeDubboContextMap1);
        assertFalse(dubboAttachmentMatch.isMatch(rpcInvocation3, Collections.singleton(tracingContextProvider3)));


        Map<String, String> invokeTracingContextMap2 = new HashMap<>();
        invokeTracingContextMap2.put("machineGroup", "test_host");
        invokeTracingContextMap2.put("other", "other");

        TracingContextProvider tracingContextProvider4 = (invocation, key) -> invokeTracingContextMap2.get(key);
        RpcInvocation rpcInvocation4 = new RpcInvocation();
        rpcInvocation4.setAttachments(invokeDubboContextMap);
        assertFalse(dubboAttachmentMatch.isMatch(rpcInvocation4, Collections.singleton(tracingContextProvider4)));

        Map<String, String> invokeDubboContextMap2 = new HashMap<>();
        TracingContextProvider tracingContextProvider5 = (invocation, key) -> invokeTracingContextMap.get(key);
        RpcInvocation rpcInvocation5 = new RpcInvocation();
        rpcInvocation5.setAttachments(invokeDubboContextMap2);
        assertFalse(dubboAttachmentMatch.isMatch(rpcInvocation5, Collections.singleton(tracingContextProvider5)));


        TracingContextProvider tracingContextProvider6 = (invocation, key) -> invokeTracingContextMap2.get(key);
        RpcInvocation rpcInvocation6 = new RpcInvocation();
        rpcInvocation5.setAttachments(invokeDubboContextMap2);
        assertFalse(dubboAttachmentMatch.isMatch(rpcInvocation6, Collections.singleton(tracingContextProvider6)));

    }
}
