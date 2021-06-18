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


import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class DubboAttachmentMatchTest {

    @Test
    public void dubboContextMatch() {
        DubboAttachmentMatch dubboAttachmentMatch = new DubboAttachmentMatch();

        Map<String, StringMatch> dubbocontextMatchMap = new HashMap<>();

        StringMatch nameMatch = new StringMatch();
        nameMatch.setExact("qinliujie");
        dubbocontextMatchMap.put("name", nameMatch);


        StringMatch machineGroupMatch = new StringMatch();
        machineGroupMatch.setExact("test_host");
        dubbocontextMatchMap.put("machineGroup", machineGroupMatch);

        dubboAttachmentMatch.setDubbocontext(dubbocontextMatchMap);

        Map<String, String> invokeDubboContextMap = new HashMap<>();
        invokeDubboContextMap.put("name", "qinliujie");
        invokeDubboContextMap.put("machineGroup", "test_host");
        invokeDubboContextMap.put("other", "other");

        assertTrue(DubboAttachmentMatch.isMatch(dubboAttachmentMatch, new HashMap<>(), invokeDubboContextMap));


        Map<String, String> invokeDubboContextMap2 = new HashMap<>();
        invokeDubboContextMap2.put("name", "jack");
        invokeDubboContextMap2.put("machineGroup", "test_host");
        invokeDubboContextMap2.put("other", "other");

        assertFalse(DubboAttachmentMatch.isMatch(dubboAttachmentMatch, new HashMap<>(), invokeDubboContextMap2));


        Map<String, String> invokeDubboContextMap3 = new HashMap<>();
        invokeDubboContextMap3.put("name", "qinliujie");
        invokeDubboContextMap3.put("machineGroup", "my_host");
        invokeDubboContextMap3.put("other", "other");

        assertFalse(DubboAttachmentMatch.isMatch(dubboAttachmentMatch, new HashMap<>(), invokeDubboContextMap3));
    }


    @Test
    public void eagleEyeContextMatch() {
        DubboAttachmentMatch dubboAttachmentMatch = new DubboAttachmentMatch();

        Map<String, StringMatch> eagleeyecontextMatchMap = new HashMap<>();

        StringMatch nameMatch = new StringMatch();
        nameMatch.setExact("qinliujie");
        eagleeyecontextMatchMap.put("name", nameMatch);


        StringMatch machineGroupMatch = new StringMatch();
        machineGroupMatch.setExact("test_host");
        eagleeyecontextMatchMap.put("machineGroup", machineGroupMatch);

        dubboAttachmentMatch.setEagleeyecontext(eagleeyecontextMatchMap);

        Map<String, String> invokeEagleEyeContextMap = new HashMap<>();
        invokeEagleEyeContextMap.put("name", "qinliujie");
        invokeEagleEyeContextMap.put("machineGroup", "test_host");
        invokeEagleEyeContextMap.put("other", "other");

        assertTrue(DubboAttachmentMatch.isMatch(dubboAttachmentMatch, invokeEagleEyeContextMap, new HashMap<>()));


        Map<String, String> invokeEagleEyeContextMap2 = new HashMap<>();
        invokeEagleEyeContextMap2.put("name", "jack");
        invokeEagleEyeContextMap2.put("machineGroup", "test_host");
        invokeEagleEyeContextMap2.put("other", "other");

        assertFalse(DubboAttachmentMatch.isMatch(dubboAttachmentMatch, invokeEagleEyeContextMap2, new HashMap<>()));


        Map<String, String> invokeEagleEyeContextMap3 = new HashMap<>();
        invokeEagleEyeContextMap3.put("name", "qinliujie");
        invokeEagleEyeContextMap3.put("machineGroup", "my_host");
        invokeEagleEyeContextMap3.put("other", "other");

        assertFalse(DubboAttachmentMatch.isMatch(dubboAttachmentMatch, invokeEagleEyeContextMap3, new HashMap<>()));
    }


    @Test
    public void contextMatch() {
        DubboAttachmentMatch dubboAttachmentMatch = new DubboAttachmentMatch();

        {
            Map<String, StringMatch> eagleeyecontextMatchMap = new HashMap<>();
            StringMatch nameMatch = new StringMatch();
            nameMatch.setExact("qinliujie");
            eagleeyecontextMatchMap.put("name", nameMatch);
            dubboAttachmentMatch.setEagleeyecontext(eagleeyecontextMatchMap);
        }


        Map<String, String> invokeEagleEyeContextMap = new HashMap<>();
        invokeEagleEyeContextMap.put("name", "qinliujie");
        invokeEagleEyeContextMap.put("machineGroup", "test_host");
        invokeEagleEyeContextMap.put("other", "other");

        //-------

        {
            Map<String, StringMatch> dubboContextMatchMap = new HashMap<>();
            StringMatch dpathMatch = new StringMatch();
            dpathMatch.setExact("PRE");
            dubboContextMatchMap.put("dpath", dpathMatch);
            dubboAttachmentMatch.setDubbocontext(dubboContextMatchMap);
        }

        Map<String, String> invokeDubboContextMap = new HashMap<>();
        invokeDubboContextMap.put("dpath", "PRE");


        assertTrue(DubboAttachmentMatch.isMatch(dubboAttachmentMatch, invokeEagleEyeContextMap, invokeDubboContextMap));


        Map<String, String> invokeEagleEyeContextMap1 = new HashMap<>();
        invokeEagleEyeContextMap1.put("name", "jack");
        invokeEagleEyeContextMap1.put("machineGroup", "test_host");
        invokeEagleEyeContextMap1.put("other", "other");
        assertFalse(DubboAttachmentMatch.isMatch(dubboAttachmentMatch, invokeEagleEyeContextMap1, invokeDubboContextMap));


        Map<String, String> invokeDubboContextMap1 = new HashMap<>();
        invokeDubboContextMap1.put("dpath", "PRE-2");
        assertFalse(DubboAttachmentMatch.isMatch(dubboAttachmentMatch, invokeEagleEyeContextMap, invokeDubboContextMap1));


        assertFalse(DubboAttachmentMatch.isMatch(dubboAttachmentMatch, invokeEagleEyeContextMap1, invokeDubboContextMap1));


        Map<String, String> invokeEagleEyeContextMap2 = new HashMap<>();
        invokeEagleEyeContextMap2.put("machineGroup", "test_host");
        invokeEagleEyeContextMap2.put("other", "other");
        assertFalse(DubboAttachmentMatch.isMatch(dubboAttachmentMatch, invokeEagleEyeContextMap2, invokeDubboContextMap));

        Map<String, String> invokeDubboContextMap2 = new HashMap<>();
        assertFalse(DubboAttachmentMatch.isMatch(dubboAttachmentMatch, invokeEagleEyeContextMap, invokeDubboContextMap2));


        assertFalse(DubboAttachmentMatch.isMatch(dubboAttachmentMatch, invokeEagleEyeContextMap2, invokeDubboContextMap2));

    }
}
