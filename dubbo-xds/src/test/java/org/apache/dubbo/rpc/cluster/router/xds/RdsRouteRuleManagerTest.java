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
package org.apache.dubbo.rpc.cluster.router.xds;

import org.apache.dubbo.rpc.cluster.router.xds.rule.HTTPRouteDestination;
import org.apache.dubbo.rpc.cluster.router.xds.rule.HttpRequestMatch;
import org.apache.dubbo.rpc.cluster.router.xds.rule.XdsRouteRule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class RdsRouteRuleManagerTest {

    @BeforeEach
    public void before() {
        RdsRouteRuleManager.getRuleListeners().clear();
        RdsRouteRuleManager.getRouteDataCache().clear();
        RdsRouteRuleManager.getRdsListeners().clear();
    }


    @Test
    public void subscribeRdsTest() {
        RdsRouteRuleManager manager = new RdsRouteRuleManager();
        String domain = "testApp";
        int subscribeNum = 3;
        for (int i = 0; i < subscribeNum; i++) {
            manager.subscribeRds(domain, new XdsRouteRuleListener() {
                @Override
                public void onRuleChange(String appName, List<XdsRouteRule> xdsRouteRules) {

                }

                @Override
                public void clearRule(String appName) {

                }
            });
        }
        assertNotNull(RdsRouteRuleManager.getRdsListeners().get(domain));
        assertEquals(RdsRouteRuleManager.getRuleListeners().get(domain).size(), subscribeNum);
    }


    @Test
    public void unsubscribeRdsTest() {
        RdsRouteRuleManager manager = new RdsRouteRuleManager();
        String domain = "testApp";
        XdsRouteRuleListener listener = new XdsRouteRuleListener() {
            @Override
            public void onRuleChange(String appName, List<XdsRouteRule> xdsRouteRules) {

            }

            @Override
            public void clearRule(String appName) {

            }
        };
        manager.subscribeRds(domain, listener);
        assertNotNull(RdsRouteRuleManager.getRdsListeners().get(domain));
        assertEquals(RdsRouteRuleManager.getRuleListeners().get(domain).size(), 1);

        manager.unSubscribeRds(domain, listener);
        assertNull(RdsRouteRuleManager.getRdsListeners().get(domain));
        assertNull(RdsRouteRuleManager.getRuleListeners().get(domain));
    }


    @Test
    public void notifyRuleChangeTest() {

        Map<String, List<XdsRouteRule>> cacheData = new HashMap<>();
        String domain = "testApp";
        List<XdsRouteRule> xdsRouteRules = new ArrayList<>();
        XdsRouteRule rule = new XdsRouteRule(new HttpRequestMatch(null, null),
            new HTTPRouteDestination());
        xdsRouteRules.add(rule);

        XdsRouteRuleListener listener = new XdsRouteRuleListener() {
            @Override
            public void onRuleChange(String appName, List<XdsRouteRule> xdsRouteRules) {
                cacheData.put(appName, xdsRouteRules);
            }

            @Override
            public void clearRule(String appName) {
                cacheData.remove(appName);
            }
        };

        RdsRouteRuleManager manager = new RdsRouteRuleManager();
        manager.subscribeRds(domain, listener);
        manager.notifyRuleChange(domain, xdsRouteRules);
        assertEquals(cacheData.get(domain), xdsRouteRules);

        Map<String, List<XdsRouteRule>> cacheData2 = new HashMap<>();
        XdsRouteRuleListener listener2 = new XdsRouteRuleListener() {
            @Override
            public void onRuleChange(String appName, List<XdsRouteRule> xdsRouteRules) {
                cacheData2.put(appName, xdsRouteRules);
            }

            @Override
            public void clearRule(String appName) {
                cacheData2.remove(appName);
            }
        };
        manager.subscribeRds(domain, listener2);
        assertEquals(cacheData2.get(domain), xdsRouteRules);
        // clear
        manager.notifyRuleChange(domain, new ArrayList<>());
        assertNull(cacheData.get(domain));
    }

}
