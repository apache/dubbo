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

import org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.match.DubboAttachmentMatch;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.match.DubboMethodMatch;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.match.StringMatch;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class DubboMatchRequestTest {

    @Test
    public void isMatch() {
        DubboMatchRequest dubboMatchRequest = new DubboMatchRequest();

        // methodMatch
        {
            DubboMethodMatch dubboMethodMatch = new DubboMethodMatch();
            StringMatch nameStringMatch = new StringMatch();
            nameStringMatch.setExact("sayHello");
            dubboMethodMatch.setName_match(nameStringMatch);

            dubboMatchRequest.setMethod(dubboMethodMatch);
        }
        assertTrue(DubboMatchRequest.isMatch(dubboMatchRequest, "sayHello", new String[]{}, new Object[]{}, new HashMap(), new HashMap(), new HashMap(), new HashMap()));
        assertFalse(DubboMatchRequest.isMatch(dubboMatchRequest, "sayHi", new String[]{}, new Object[]{}, new HashMap(), new HashMap(), new HashMap(), new HashMap()));
        // sourceLabels
        {
            Map<String, String> sourceLabels = new HashMap<>();
            sourceLabels.put("key1", "value1");
            sourceLabels.put("key2", "value2");

            dubboMatchRequest.setSourceLabels(sourceLabels);
        }

        Map<String, String> inputSourceLablesMap = new HashMap<>();
        inputSourceLablesMap.put("key1", "value1");
        inputSourceLablesMap.put("key2", "value2");
        inputSourceLablesMap.put("key3", "value3");

        Map<String, String> inputSourceLablesMap2 = new HashMap<>();
        inputSourceLablesMap2.put("key1", "other");
        inputSourceLablesMap2.put("key2", "value2");
        inputSourceLablesMap2.put("key3", "value3");

        assertTrue(DubboMatchRequest.isMatch(dubboMatchRequest, "sayHello", new String[]{}, new Object[]{}, inputSourceLablesMap, new HashMap(), new HashMap(), new HashMap()));
        assertFalse(DubboMatchRequest.isMatch(dubboMatchRequest, "sayHello", new String[]{}, new Object[]{}, inputSourceLablesMap2, new HashMap(), new HashMap(), new HashMap()));


        // eagleeyeContext
        {
            DubboAttachmentMatch dubboAttachmentMatch = new DubboAttachmentMatch();

            {
                Map<String, StringMatch> eagleeyecontextMatchMap = new HashMap<>();
                StringMatch nameMatch = new StringMatch();
                nameMatch.setExact("qinliujie");
                eagleeyecontextMatchMap.put("name", nameMatch);
                dubboAttachmentMatch.setEagleeyecontext(eagleeyecontextMatchMap);
            }

            dubboMatchRequest.setAttachments(dubboAttachmentMatch);
        }

        Map<String, String> invokeEagleEyeContextMap = new HashMap<>();
        invokeEagleEyeContextMap.put("name", "qinliujie");
        invokeEagleEyeContextMap.put("machineGroup", "test_host");
        invokeEagleEyeContextMap.put("other", "other");

        assertTrue(DubboMatchRequest.isMatch(dubboMatchRequest, "sayHello", new String[]{}, new Object[]{}, inputSourceLablesMap, invokeEagleEyeContextMap, new HashMap(), new HashMap()));


        Map<String, String> invokeEagleEyeContextMap2 = new HashMap<>();
        invokeEagleEyeContextMap2.put("name", "jack");
        invokeEagleEyeContextMap2.put("machineGroup", "test_host");
        invokeEagleEyeContextMap2.put("other", "other");

        assertFalse(DubboMatchRequest.isMatch(dubboMatchRequest, "sayHello", new String[]{}, new Object[]{}, inputSourceLablesMap, invokeEagleEyeContextMap2, new HashMap(), new HashMap()));


        //dubbo context

        {
            DubboAttachmentMatch dubboAttachmentMatch = new DubboAttachmentMatch();

            {
                Map<String, StringMatch> eagleeyecontextMatchMap = new HashMap<>();
                StringMatch nameMatch = new StringMatch();
                nameMatch.setExact("qinliujie");
                eagleeyecontextMatchMap.put("name", nameMatch);
                dubboAttachmentMatch.setEagleeyecontext(eagleeyecontextMatchMap);
            }


            {
                Map<String, StringMatch> dubboContextMatchMap = new HashMap<>();
                StringMatch dpathMatch = new StringMatch();
                dpathMatch.setExact("PRE");
                dubboContextMatchMap.put("dpath", dpathMatch);
                dubboAttachmentMatch.setDubbocontext(dubboContextMatchMap);
            }

            dubboMatchRequest.setAttachments(dubboAttachmentMatch);
        }


        Map<String, String> invokeDubboContextMap = new HashMap<>();
        invokeDubboContextMap.put("dpath", "PRE");

        assertTrue(DubboMatchRequest.isMatch(dubboMatchRequest, "sayHello", new String[]{}, new Object[]{}, inputSourceLablesMap, invokeEagleEyeContextMap, invokeDubboContextMap, new HashMap()));


        Map<String, String> invokeDubboContextMap2 = new HashMap<>();
        invokeDubboContextMap.put("dpath", "other");

        assertFalse(DubboMatchRequest.isMatch(dubboMatchRequest, "sayHello", new String[]{}, new Object[]{}, inputSourceLablesMap, invokeEagleEyeContextMap, invokeDubboContextMap2, new HashMap()));
    }
}
