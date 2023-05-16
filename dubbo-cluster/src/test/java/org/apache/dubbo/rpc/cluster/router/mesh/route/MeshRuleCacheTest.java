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
package org.apache.dubbo.rpc.cluster.router.mesh.route;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.VsDestinationGroup;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.destination.DestinationRule;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.destination.DestinationRuleSpec;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.destination.Subset;
import org.apache.dubbo.rpc.cluster.router.state.BitList;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MeshRuleCacheTest {

    private Invoker<Object> createInvoker(String app) {
        URL url = URL.valueOf("dubbo://localhost/DemoInterface?" + (StringUtils.isEmpty(app) ? "" : "remote.application=" + app));
        Invoker invoker = Mockito.mock(Invoker.class);
        when(invoker.getUrl()).thenReturn(url);
        return invoker;
    }

    @Test
    void containMapKeyValue() {
        URL url = mock(URL.class);
        when(url.getOriginalServiceParameter("test", "key1")).thenReturn("value1");
        when(url.getOriginalServiceParameter("test", "key2")).thenReturn("value2");
        when(url.getOriginalServiceParameter("test", "key3")).thenReturn("value3");
        when(url.getOriginalServiceParameter("test", "key4")).thenReturn("value4");


        Map<String, String> originMap = new HashMap<>();

        originMap.put("key1", "value1");
        originMap.put("key2", "value2");
        originMap.put("key3", "value3");

        Map<String, String> inputMap = new HashMap<>();

        inputMap.put("key1", "value1");
        inputMap.put("key2", "value2");

        assertTrue(MeshRuleCache.isLabelMatch(url, "test", inputMap));

        inputMap.put("key4", "value4");
        assertTrue(MeshRuleCache.isLabelMatch(url, "test", inputMap));

    }


    @Test
    void testBuild() {
        BitList<Invoker<Object>> invokers = new BitList<>(Arrays.asList(createInvoker(""), createInvoker("unknown"), createInvoker("app1")));

        Subset subset = new Subset();
        subset.setName("TestSubset");
        DestinationRule destinationRule = new DestinationRule();
        DestinationRuleSpec destinationRuleSpec = new DestinationRuleSpec();
        destinationRuleSpec.setSubsets(Collections.singletonList(subset));
        destinationRule.setSpec(destinationRuleSpec);
        VsDestinationGroup vsDestinationGroup = new VsDestinationGroup();
        vsDestinationGroup.getDestinationRuleList().add(destinationRule);
        Map<String, VsDestinationGroup> vsDestinationGroupMap = new HashMap<>();
        vsDestinationGroupMap.put("app1", vsDestinationGroup);

        MeshRuleCache<Object> cache = MeshRuleCache.build("test", invokers, vsDestinationGroupMap);
        assertEquals(2, cache.getUnmatchedInvokers().size());
        assertEquals(1, cache.getSubsetInvokers("app1", "TestSubset").size());

        subset.setLabels(Collections.singletonMap("test", "test"));
        cache = MeshRuleCache.build("test", invokers, vsDestinationGroupMap);
        assertEquals(3, cache.getUnmatchedInvokers().size());
        assertEquals(0, cache.getSubsetInvokers("app1", "TestSubset").size());

        invokers = new BitList<>(Arrays.asList(createInvoker(""), createInvoker("unknown"), createInvoker("app1"), createInvoker("app2")));
        subset.setLabels(null);
        cache = MeshRuleCache.build("test", invokers, vsDestinationGroupMap);
        assertEquals(3, cache.getUnmatchedInvokers().size());
        assertEquals(1, cache.getSubsetInvokers("app1", "TestSubset").size());
        assertEquals(0, cache.getSubsetInvokers("app2", "TestSubset").size());
    }
}
