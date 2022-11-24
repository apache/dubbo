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

package org.apache.dubbo.rpc.cluster.router.mesh.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

class MeshRuleDispatcherTest {

    @Test
    void post() {
        MeshRuleDispatcher meshRuleDispatcher = new MeshRuleDispatcher("TestApp");

        Map<String, List<Map<String, Object>>> ruleMap = new HashMap<>();
        List<Map<String, Object>> type1 = new LinkedList<>();
        List<Map<String, Object>> type2 = new LinkedList<>();
        List<Map<String, Object>> type3 = new LinkedList<>();
        ruleMap.put("Type1", type1);
        ruleMap.put("Type2", type2);
        ruleMap.put("Type3", type3);

        AtomicInteger count = new AtomicInteger(0);
        MeshRuleListener listener1 = new MeshRuleListener() {
            @Override
            public void onRuleChange(String appName, List<Map<String, Object>> rules) {
                Assertions.assertEquals("TestApp", appName);
                Assertions.assertEquals(System.identityHashCode(type1), System.identityHashCode(rules));
                count.incrementAndGet();
            }

            @Override
            public void clearRule(String appName) {

            }

            @Override
            public String ruleSuffix() {
                return "Type1";
            }
        };

        MeshRuleListener listener2 = new MeshRuleListener() {
            @Override
            public void onRuleChange(String appName, List<Map<String, Object>> rules) {
                Assertions.assertEquals("TestApp", appName);
                Assertions.assertEquals(System.identityHashCode(type2), System.identityHashCode(rules));
                count.incrementAndGet();
            }

            @Override
            public void clearRule(String appName) {

            }

            @Override
            public String ruleSuffix() {
                return "Type2";
            }
        };

        MeshRuleListener listener4 = new MeshRuleListener() {
            @Override
            public void onRuleChange(String appName, List<Map<String, Object>> rules) {
                Assertions.fail();
            }

            @Override
            public void clearRule(String appName) {
                Assertions.assertEquals("TestApp", appName);
                count.incrementAndGet();
            }

            @Override
            public String ruleSuffix() {
                return "Type4";
            }
        };

        meshRuleDispatcher.register(listener1);
        meshRuleDispatcher.register(listener2);
        meshRuleDispatcher.register(listener4);

        meshRuleDispatcher.post(ruleMap);

        Assertions.assertEquals(3, count.get());
    }

    @Test
    void register() {
        MeshRuleDispatcher meshRuleDispatcher = new MeshRuleDispatcher("TestApp");

        MeshRuleListener listener1 = new MeshRuleListener() {
            @Override
            public void onRuleChange(String appName, List<Map<String, Object>> rules) {
            }

            @Override
            public void clearRule(String appName) {

            }

            @Override
            public String ruleSuffix() {
                return "Type1";
            }
        };

        meshRuleDispatcher.register(listener1);
        meshRuleDispatcher.register(listener1);

        Assertions.assertEquals(1, meshRuleDispatcher.getListenerMap().get("Type1").size());
        Assertions.assertTrue(meshRuleDispatcher.getListenerMap().get("Type1").contains(listener1));
    }

    @Test
    void unregister() {
        MeshRuleDispatcher meshRuleDispatcher = new MeshRuleDispatcher("TestApp");

        MeshRuleListener listener1 = new MeshRuleListener() {
            @Override
            public void onRuleChange(String appName, List<Map<String, Object>> rules) {
            }

            @Override
            public void clearRule(String appName) {

            }

            @Override
            public String ruleSuffix() {
                return "Type1";
            }
        };

        MeshRuleListener listener2 = new MeshRuleListener() {
            @Override
            public void onRuleChange(String appName, List<Map<String, Object>> rules) {
            }

            @Override
            public void clearRule(String appName) {

            }

            @Override
            public String ruleSuffix() {
                return "Type1";
            }
        };

        MeshRuleListener listener3 = new MeshRuleListener() {
            @Override
            public void onRuleChange(String appName, List<Map<String, Object>> rules) {
            }

            @Override
            public void clearRule(String appName) {

            }

            @Override
            public String ruleSuffix() {
                return "Type2";
            }
        };

        meshRuleDispatcher.register(listener1);
        meshRuleDispatcher.register(listener2);
        meshRuleDispatcher.register(listener3);

        Assertions.assertEquals(2, meshRuleDispatcher.getListenerMap().get("Type1").size());
        Assertions.assertTrue(meshRuleDispatcher.getListenerMap().get("Type1").contains(listener1));
        Assertions.assertTrue(meshRuleDispatcher.getListenerMap().get("Type1").contains(listener2));
        Assertions.assertEquals(1, meshRuleDispatcher.getListenerMap().get("Type2").size());
        Assertions.assertTrue(meshRuleDispatcher.getListenerMap().get("Type2").contains(listener3));

        meshRuleDispatcher.unregister(listener1);
        Assertions.assertEquals(1, meshRuleDispatcher.getListenerMap().get("Type1").size());
        Assertions.assertTrue(meshRuleDispatcher.getListenerMap().get("Type1").contains(listener2));
        Assertions.assertEquals(1, meshRuleDispatcher.getListenerMap().get("Type2").size());
        Assertions.assertTrue(meshRuleDispatcher.getListenerMap().get("Type2").contains(listener3));

        meshRuleDispatcher.unregister(listener2);
        Assertions.assertNull(meshRuleDispatcher.getListenerMap().get("Type1"));
        Assertions.assertEquals(1, meshRuleDispatcher.getListenerMap().get("Type2").size());
        Assertions.assertTrue(meshRuleDispatcher.getListenerMap().get("Type2").contains(listener3));

        meshRuleDispatcher.unregister(listener3);
        Assertions.assertNull(meshRuleDispatcher.getListenerMap().get("Type1"));
        Assertions.assertNull(meshRuleDispatcher.getListenerMap().get("Type2"));
    }
}
