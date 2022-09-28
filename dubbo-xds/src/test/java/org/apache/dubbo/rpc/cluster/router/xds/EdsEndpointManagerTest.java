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

import org.apache.dubbo.registry.xds.util.protocol.message.Endpoint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class EdsEndpointManagerTest {


    @BeforeEach
    public void before() {
        EdsEndpointManager.getEdsListeners().clear();
        EdsEndpointManager.getEndpointListeners().clear();
        EdsEndpointManager.getEndpointDataCache().clear();
    }

    @Test
    public void subscribeEdsTest() {
        EdsEndpointManager manager = new EdsEndpointManager();
        String cluster = "testApp";
        int subscribeNum = 3;
        for (int i = 0; i < subscribeNum; i++) {
            manager.subscribeEds(cluster, new EdsEndpointListener() {
                @Override
                public void onEndPointChange(String cluster, Set<Endpoint> endpoints) {

                }
            });
        }
        assertNotNull(EdsEndpointManager.getEdsListeners().get(cluster));
        assertEquals(EdsEndpointManager.getEndpointListeners().get(cluster).size(), subscribeNum);
    }


    @Test
    public void unsubscribeRdsTest() {
        EdsEndpointManager manager = new EdsEndpointManager();
        String domain = "testApp";
        EdsEndpointListener listener = new EdsEndpointListener() {
            @Override
            public void onEndPointChange(String cluster, Set<Endpoint> endpoints) {

            }
        };
        manager.subscribeEds(domain, listener);
        assertNotNull(EdsEndpointManager.getEdsListeners().get(domain));
        assertEquals(EdsEndpointManager.getEndpointListeners().get(domain).size(), 1);

        manager.unSubscribeEds(domain, listener);
        assertNull(EdsEndpointManager.getEdsListeners().get(domain));
        assertNull(EdsEndpointManager.getEndpointListeners().get(domain));
    }


    @Test
    public void notifyRuleChangeTest() {

        Map<String, Set<Endpoint>> cacheData = new HashMap<>();
        String domain = "testApp";
        Set<Endpoint> endpoints = new HashSet<>();
        Endpoint endpoint = new Endpoint();
        endpoints.add(endpoint);

        EdsEndpointListener listener = new EdsEndpointListener() {
            @Override
            public void onEndPointChange(String cluster, Set<Endpoint> endpoints) {
                cacheData.put(cluster, endpoints);
            }
        };

        EdsEndpointManager manager = new EdsEndpointManager();
        manager.subscribeEds(domain, listener);
        manager.notifyEndpointChange(domain, endpoints);
        assertEquals(cacheData.get(domain), endpoints);

        Map<String, Set<Endpoint>> cacheData2 = new HashMap<>();
        EdsEndpointListener listener2 = new EdsEndpointListener() {
            @Override
            public void onEndPointChange(String cluster, Set<Endpoint> endpoints) {
                cacheData2.put(cluster, endpoints);
            }
        };
        manager.subscribeEds(domain, listener2);
        assertEquals(cacheData2.get(domain), endpoints);
        // clear
        manager.notifyEndpointChange(domain, new HashSet<>());
        assertEquals(cacheData.get(domain).size(), 0);
    }

}
