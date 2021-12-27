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
import org.apache.dubbo.common.config.configcenter.ConfigChangeType;
import org.apache.dubbo.common.config.configcenter.ConfigChangedEvent;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.rpc.cluster.router.mesh.util.MeshRuleListener;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.dubbo.rpc.cluster.router.mesh.route.MeshRuleConstants.MESH_RULE_DATA_ID_SUFFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


public class MeshAppRuleListenerTest {

    private final static String rule1 = "apiVersion: service.dubbo.apache.org/v1alpha1\n" +
        "kind: DestinationRule\n" +
        "metadata: { name: demo-route }\n" +
        "spec:\n" +
        "  host: demo\n" +
        "  subsets:\n" +
        "    - labels: { env-sign: xxx, tag1: hello }\n" +
        "      name: isolation\n" +
        "    - labels: { env-sign: yyy }\n" +
        "      name: testing-trunk\n" +
        "    - labels: { env-sign: zzz }\n" +
        "      name: testing\n" +
        "  trafficPolicy:\n" +
        "    loadBalancer: { simple: ROUND_ROBIN }\n" +
        "\n";
    private final static String rule2 = "apiVersion: service.dubbo.apache.org/v1alpha1\n" +
        "kind: VirtualService\n" +
        "metadata: { name: demo-route }\n" +
        "spec:\n" +
        "  dubbo:\n" +
        "    - routedetail:\n" +
        "        - match:\n" +
        "            - sourceLabels: {trafficLabel: xxx}\n" +
        "          name: xxx-project\n" +
        "          route:\n" +
        "            - destination: {host: demo, subset: isolation}\n" +
        "        - match:\n" +
        "            - sourceLabels: {trafficLabel: testing-trunk}\n" +
        "          name: testing-trunk\n" +
        "          route:\n" +
        "            - destination: {host: demo, subset: testing-trunk}\n" +
        "        - name: testing\n" +
        "          route:\n" +
        "            - destination: {host: demo, subset: testing}\n" +
        "      services:\n" +
        "        - {regex: ccc}\n" +
        "  hosts: [demo]\n";
    private final static String rule3 = "apiVersion: service.dubbo.apache.org/v1alpha1\n" +
        "kind: DestinationRule\n" +
        "spec:\n" +
        "  host: demo\n" +
        "  subsets:\n" +
        "    - labels: { env-sign: xxx, tag1: hello }\n" +
        "      name: isolation\n" +
        "    - labels: { env-sign: yyy }\n" +
        "      name: testing-trunk\n" +
        "    - labels: { env-sign: zzz }\n" +
        "      name: testing\n" +
        "  trafficPolicy:\n" +
        "    loadBalancer: { simple: ROUND_ROBIN }\n";
    private final static String rule4 = "apiVersionservice.dubbo.apache.org/v1alpha1\n";
    private final static String rule5 = "apiVersion: service.dubbo.apache.org/v1alpha1\n" +
        "kind: DestinationRule\n" +
        "metadata: { name: demo-route.Type1 }\n" +
        "spec:\n" +
        "  host: demo\n" +
        "\n";
    private final static String rule6 = "apiVersion: service.dubbo.apache.org/v1alpha1\n" +
        "kind: VirtualService\n" +
        "metadata: { name: demo-route.Type1 }\n" +
        "spec:\n" +
        "  hosts: [demo]\n";
    private final static String rule7 = "apiVersion: service.dubbo.apache.org/v1alpha1\n" +
        "kind: DestinationRule\n" +
        "metadata: { name: demo-route.Type2 }\n" +
        "spec:\n" +
        "  host: demo\n" +
        "\n";
    private final static String rule8 = "apiVersion: service.dubbo.apache.org/v1alpha1\n" +
        "kind: VirtualService\n" +
        "metadata: { name: demo-route.Type2 }\n" +
        "spec:\n" +
        "  hosts: [demo]\n";

    @Test
    public void testStandard() {
        MeshAppRuleListener meshAppRuleListener = new MeshAppRuleListener("demo-route");

        StandardMeshRuleRouter standardMeshRuleRouter = Mockito.spy(new StandardMeshRuleRouter(URL.valueOf("")));
        meshAppRuleListener.register(standardMeshRuleRouter);

        meshAppRuleListener.receiveConfigInfo(rule1 + "---\n" + rule2);

        ArgumentCaptor<String> appCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<Map<String, Object>>> ruleCaptor = ArgumentCaptor.forClass(List.class);

        verify(standardMeshRuleRouter, times(1)).onRuleChange(appCaptor.capture(), ruleCaptor.capture());

        List<Map<String, Object>> rulesReceived = ruleCaptor.getValue();
        assertEquals(2, rulesReceived.size());
        Yaml yaml = new Yaml(new SafeConstructor());
        Assertions.assertTrue(rulesReceived.contains(yaml.load(rule1)));
        Assertions.assertTrue(rulesReceived.contains(yaml.load(rule2)));

        Assertions.assertEquals("demo-route", appCaptor.getValue());

        meshAppRuleListener.receiveConfigInfo("");
        verify(standardMeshRuleRouter, times(1)).clearRule("demo-route");
    }

    @Test
    public void register() {
        MeshAppRuleListener meshAppRuleListener = new MeshAppRuleListener("demo-route");

        StandardMeshRuleRouter standardMeshRuleRouter1 = Mockito.spy(new StandardMeshRuleRouter(URL.valueOf("")));
        StandardMeshRuleRouter standardMeshRuleRouter2 = Mockito.spy(new StandardMeshRuleRouter(URL.valueOf("")));

        meshAppRuleListener.register(standardMeshRuleRouter1);

        Assertions.assertEquals(1, meshAppRuleListener.getMeshRuleDispatcher().getListenerMap().get(MeshRuleConstants.STANDARD_ROUTER_KEY).size());
        meshAppRuleListener.receiveConfigInfo(rule1 + "---\n" + rule2);
        meshAppRuleListener.register(standardMeshRuleRouter2);
        Assertions.assertEquals(2, meshAppRuleListener.getMeshRuleDispatcher().getListenerMap().get(MeshRuleConstants.STANDARD_ROUTER_KEY).size());

        ArgumentCaptor<String> appCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<Map<String, Object>>> ruleCaptor = ArgumentCaptor.forClass(List.class);

        verify(standardMeshRuleRouter1, times(1)).onRuleChange(appCaptor.capture(), ruleCaptor.capture());

        List<Map<String, Object>> rulesReceived = ruleCaptor.getValue();
        assertEquals(2, rulesReceived.size());
        Yaml yaml = new Yaml(new SafeConstructor());
        Assertions.assertTrue(rulesReceived.contains(yaml.load(rule1)));
        Assertions.assertTrue(rulesReceived.contains(yaml.load(rule2)));

        Assertions.assertEquals("demo-route", appCaptor.getValue());

        verify(standardMeshRuleRouter2, times(1)).onRuleChange(appCaptor.capture(), ruleCaptor.capture());
        rulesReceived = ruleCaptor.getValue();
        assertEquals(2, rulesReceived.size());
        Assertions.assertTrue(rulesReceived.contains(yaml.load(rule1)));
        Assertions.assertTrue(rulesReceived.contains(yaml.load(rule2)));

        Assertions.assertEquals("demo-route", appCaptor.getValue());

    }

    @Test
    public void unregister() {
        MeshAppRuleListener meshAppRuleListener = new MeshAppRuleListener("demo-route");

        StandardMeshRuleRouter standardMeshRuleRouter1 = Mockito.spy(new StandardMeshRuleRouter(URL.valueOf("")));
        StandardMeshRuleRouter standardMeshRuleRouter2 = Mockito.spy(new StandardMeshRuleRouter(URL.valueOf("")));

        meshAppRuleListener.register(standardMeshRuleRouter1);

        Assertions.assertEquals(1, meshAppRuleListener.getMeshRuleDispatcher().getListenerMap().get(MeshRuleConstants.STANDARD_ROUTER_KEY).size());
        meshAppRuleListener.receiveConfigInfo(rule1 + "---\n" + rule2);
        meshAppRuleListener.register(standardMeshRuleRouter2);
        Assertions.assertEquals(2, meshAppRuleListener.getMeshRuleDispatcher().getListenerMap().get(MeshRuleConstants.STANDARD_ROUTER_KEY).size());

        meshAppRuleListener.unregister(standardMeshRuleRouter1);
        Assertions.assertEquals(1, meshAppRuleListener.getMeshRuleDispatcher().getListenerMap().get(MeshRuleConstants.STANDARD_ROUTER_KEY).size());

        meshAppRuleListener.unregister(standardMeshRuleRouter2);
        Assertions.assertEquals(0, meshAppRuleListener.getMeshRuleDispatcher().getListenerMap().size());
    }

    @Test
    public void process() {
        MeshAppRuleListener meshAppRuleListener = new MeshAppRuleListener("demo-route");

        StandardMeshRuleRouter standardMeshRuleRouter = Mockito.spy(new StandardMeshRuleRouter(URL.valueOf("")));
        meshAppRuleListener.register(standardMeshRuleRouter);

        ConfigChangedEvent configChangedEvent = new ConfigChangedEvent("demo-route" + MESH_RULE_DATA_ID_SUFFIX, DynamicConfiguration.DEFAULT_GROUP,
            rule1 + "---\n" + rule2, ConfigChangeType.ADDED);

        meshAppRuleListener.process(configChangedEvent);

        ArgumentCaptor<String> appCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<Map<String, Object>>> ruleCaptor = ArgumentCaptor.forClass(List.class);

        verify(standardMeshRuleRouter, times(1)).onRuleChange(appCaptor.capture(), ruleCaptor.capture());

        List<Map<String, Object>> rulesReceived = ruleCaptor.getValue();
        assertEquals(2, rulesReceived.size());
        Yaml yaml = new Yaml(new SafeConstructor());
        Assertions.assertTrue(rulesReceived.contains(yaml.load(rule1)));
        Assertions.assertTrue(rulesReceived.contains(yaml.load(rule2)));

        configChangedEvent = new ConfigChangedEvent("demo-route" + MESH_RULE_DATA_ID_SUFFIX, DynamicConfiguration.DEFAULT_GROUP,
            rule1 + "---\n" + rule2, ConfigChangeType.MODIFIED);

        meshAppRuleListener.process(configChangedEvent);

        verify(standardMeshRuleRouter, times(2)).onRuleChange(appCaptor.capture(), ruleCaptor.capture());

        rulesReceived = ruleCaptor.getValue();
        assertEquals(2, rulesReceived.size());
        Assertions.assertTrue(rulesReceived.contains(yaml.load(rule1)));
        Assertions.assertTrue(rulesReceived.contains(yaml.load(rule2)));

        configChangedEvent = new ConfigChangedEvent("demo-route" + MESH_RULE_DATA_ID_SUFFIX, DynamicConfiguration.DEFAULT_GROUP,
            "", ConfigChangeType.DELETED);
        meshAppRuleListener.process(configChangedEvent);

        verify(standardMeshRuleRouter, times(1)).clearRule("demo-route");
    }

    @Test
    public void testUnknownRule() {
        MeshAppRuleListener meshAppRuleListener = new MeshAppRuleListener("demo-route");

        StandardMeshRuleRouter standardMeshRuleRouter = Mockito.spy(new StandardMeshRuleRouter(URL.valueOf("")));

        meshAppRuleListener.register(standardMeshRuleRouter);

        meshAppRuleListener.receiveConfigInfo(rule3 + "---\n" + rule2);
        ArgumentCaptor<String> appCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<Map<String, Object>>> ruleCaptor = ArgumentCaptor.forClass(List.class);

        verify(standardMeshRuleRouter, times(1)).onRuleChange(appCaptor.capture(), ruleCaptor.capture());

        List<Map<String, Object>> rulesReceived = ruleCaptor.getValue();
        assertEquals(1, rulesReceived.size());
        Yaml yaml = new Yaml(new SafeConstructor());
        Assertions.assertTrue(rulesReceived.contains(yaml.load(rule2)));

        meshAppRuleListener.receiveConfigInfo(rule1 + "---\n" + rule4);

        verify(standardMeshRuleRouter, times(2)).onRuleChange(appCaptor.capture(), ruleCaptor.capture());

        rulesReceived = ruleCaptor.getValue();
        assertEquals(1, rulesReceived.size());
        Assertions.assertTrue(rulesReceived.contains(yaml.load(rule1)));

        meshAppRuleListener.receiveConfigInfo(rule3 + "---\n" + rule4);
        verify(standardMeshRuleRouter, times(1)).clearRule("demo-route");
    }

    @Test
    public void testMultipleRule() {
        MeshAppRuleListener meshAppRuleListener = new MeshAppRuleListener("demo-route");

        AtomicInteger count = new AtomicInteger(0);
        MeshRuleListener listener1 = new MeshRuleListener() {
            @Override
            public void onRuleChange(String appName, List<Map<String, Object>> rules) {
                Assertions.assertEquals("demo-route", appName);
                Yaml yaml = new Yaml(new SafeConstructor());
                Assertions.assertTrue(rules.contains(yaml.load(rule5)));
                Assertions.assertTrue(rules.contains(yaml.load(rule6)));
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
                Assertions.assertEquals("demo-route", appName);
                Yaml yaml = new Yaml(new SafeConstructor());
                Assertions.assertTrue(rules.contains(yaml.load(rule7)));
                Assertions.assertTrue(rules.contains(yaml.load(rule8)));
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
                Assertions.assertEquals("demo-route", appName);
                count.incrementAndGet();
            }

            @Override
            public String ruleSuffix() {
                return "Type4";
            }
        };

        StandardMeshRuleRouter standardMeshRuleRouter = Mockito.spy(new StandardMeshRuleRouter(URL.valueOf("")));

        meshAppRuleListener.register(standardMeshRuleRouter);
        meshAppRuleListener.register(listener1);
        meshAppRuleListener.register(listener2);
        meshAppRuleListener.register(listener4);

        meshAppRuleListener.receiveConfigInfo(rule1 + "---\n" + rule2 + "---\n" + rule5 + "---\n" + rule6 + "---\n" + rule7 + "---\n" + rule8);
        ArgumentCaptor<String> appCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<Map<String, Object>>> ruleCaptor = ArgumentCaptor.forClass(List.class);

        verify(standardMeshRuleRouter, times(1)).onRuleChange(appCaptor.capture(), ruleCaptor.capture());

        List<Map<String, Object>> rulesReceived = ruleCaptor.getValue();
        assertEquals(2, rulesReceived.size());
        Yaml yaml = new Yaml(new SafeConstructor());
        Assertions.assertTrue(rulesReceived.contains(yaml.load(rule1)));
        Assertions.assertTrue(rulesReceived.contains(yaml.load(rule2)));

        Assertions.assertEquals("demo-route", appCaptor.getValue());

        Assertions.assertEquals(3, count.get());
    }
}
