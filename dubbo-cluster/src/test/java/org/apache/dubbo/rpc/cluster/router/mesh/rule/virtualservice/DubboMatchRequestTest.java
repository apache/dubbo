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

package org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice;

import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.match.DubboAttachmentMatch;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.match.DubboMethodMatch;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.match.StringMatch;
import org.apache.dubbo.rpc.cluster.router.mesh.util.TracingContextProvider;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class DubboMatchRequestTest {

    @Test
    public void isMatch() {
        DubboMatchRequest dubboMatchRequest = new DubboMatchRequest();

        // methodMatch
        DubboMethodMatch dubboMethodMatch = new DubboMethodMatch();
        StringMatch nameStringMatch = new StringMatch();
        nameStringMatch.setExact("sayHello");
        dubboMethodMatch.setName_match(nameStringMatch);

        dubboMatchRequest.setMethod(dubboMethodMatch);

        RpcInvocation rpcInvocation = new RpcInvocation();
        rpcInvocation.setMethodName("sayHello");
        assertTrue(dubboMatchRequest.isMatch(rpcInvocation, new HashMap<>(), Collections.emptySet()));

        rpcInvocation.setMethodName("satHi");
        assertFalse(dubboMatchRequest.isMatch(rpcInvocation, new HashMap<>(), Collections.emptySet()));

        // sourceLabels
        Map<String, String> sourceLabels = new HashMap<>();
        sourceLabels.put("key1", "value1");
        sourceLabels.put("key2", "value2");

        dubboMatchRequest.setSourceLabels(sourceLabels);

        Map<String, String> inputSourceLabelsMap = new HashMap<>();
        inputSourceLabelsMap.put("key1", "value1");
        inputSourceLabelsMap.put("key2", "value2");
        inputSourceLabelsMap.put("key3", "value3");

        Map<String, String> inputSourceLabelsMap2 = new HashMap<>();
        inputSourceLabelsMap2.put("key1", "other");
        inputSourceLabelsMap2.put("key2", "value2");
        inputSourceLabelsMap2.put("key3", "value3");

        rpcInvocation.setMethodName("sayHello");
        assertTrue(dubboMatchRequest.isMatch(rpcInvocation, inputSourceLabelsMap, Collections.emptySet()));
        assertFalse(dubboMatchRequest.isMatch(rpcInvocation, inputSourceLabelsMap2, Collections.emptySet()));


        // tracingContext
        DubboAttachmentMatch dubboAttachmentMatch = new DubboAttachmentMatch();
        Map<String, StringMatch> tracingContextMatchMap = new HashMap<>();
        StringMatch nameMatch = new StringMatch();
        nameMatch.setExact("qinliujie");
        tracingContextMatchMap.put("name", nameMatch);
        dubboAttachmentMatch.setTracingContext(tracingContextMatchMap);
        dubboMatchRequest.setAttachments(dubboAttachmentMatch);

        Map<String, String> invokeTracingContextMap = new HashMap<>();
        invokeTracingContextMap.put("name", "qinliujie");
        invokeTracingContextMap.put("machineGroup", "test_host");
        invokeTracingContextMap.put("other", "other");

        TracingContextProvider tracingContextProvider = (invocation, key) -> invokeTracingContextMap.get(key);
        assertTrue(dubboMatchRequest.isMatch(rpcInvocation, inputSourceLabelsMap, Collections.singleton(tracingContextProvider)));


        Map<String, String> invokeTracingContextMap2 = new HashMap<>();
        invokeTracingContextMap2.put("name", "jack");
        invokeTracingContextMap2.put("machineGroup", "test_host");
        invokeTracingContextMap2.put("other", "other");

        TracingContextProvider tracingContextProvider2 = (invocation, key) -> invokeTracingContextMap2.get(key);
        assertFalse(dubboMatchRequest.isMatch(rpcInvocation, inputSourceLabelsMap, Collections.singleton(tracingContextProvider2)));


        //dubbo context
        dubboAttachmentMatch = new DubboAttachmentMatch();

        Map<String, StringMatch> eagleeyecontextMatchMap = new HashMap<>();
        nameMatch = new StringMatch();
        nameMatch.setExact("qinliujie");
        eagleeyecontextMatchMap.put("name", nameMatch);
        dubboAttachmentMatch.setTracingContext(eagleeyecontextMatchMap);


        Map<String, StringMatch> dubboContextMatchMap = new HashMap<>();
        StringMatch dpathMatch = new StringMatch();
        dpathMatch.setExact("PRE");
        dubboContextMatchMap.put("dpath", dpathMatch);
        dubboAttachmentMatch.setDubboContext(dubboContextMatchMap);

        dubboMatchRequest.setAttachments(dubboAttachmentMatch);


        Map<String, String> invokeDubboContextMap = new HashMap<>();
        invokeDubboContextMap.put("dpath", "PRE");

        rpcInvocation.setAttachments(invokeDubboContextMap);
        TracingContextProvider tracingContextProvider3 = (invocation, key) -> invokeTracingContextMap.get(key);
        assertTrue(dubboMatchRequest.isMatch(rpcInvocation, inputSourceLabelsMap, Collections.singleton(tracingContextProvider3)));

        Map<String, String> invokeDubboContextMap2 = new HashMap<>();
        invokeDubboContextMap.put("dpath", "other");

        rpcInvocation.setAttachments(invokeDubboContextMap2);
        assertFalse(dubboMatchRequest.isMatch(rpcInvocation, inputSourceLabelsMap, Collections.singleton(tracingContextProvider3)));
    }
}
