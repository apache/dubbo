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

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.cluster.governance.GovernanceRuleRepository;
import org.apache.dubbo.rpc.cluster.router.mesh.util.MeshRuleListener;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class MeshRuleManagerTest {
    private final static String rule1 = "apiVersion: service.dubbo.apache.org/v1alpha1\n" +
        "kind: DestinationRule\n" +
        "metadata: { name: demo-route.Type1 }\n" +
        "spec:\n" +
        "  host: demo\n" +
        "\n";
    private final static String rule2 = "apiVersion: service.dubbo.apache.org/v1alpha1\n" +
        "kind: VirtualService\n" +
        "metadata: { name: demo-route.Type1 }\n" +
        "spec:\n" +
        "  hosts: [demo]\n";
    private final static String rule3 = "apiVersion: service.dubbo.apache.org/v1alpha1\n" +
        "kind: DestinationRule\n" +
        "metadata: { name: demo-route.Type2 }\n" +
        "spec:\n" +
        "  host: demo\n" +
        "\n";
    private final static String rule4 = "apiVersion: service.dubbo.apache.org/v1alpha1\n" +
        "kind: VirtualService\n" +
        "metadata: { name: demo-route.Type2 }\n" +
        "spec:\n" +
        "  hosts: [demo]\n";


    private ModuleModel originModule;
    private ModuleModel moduleModel;
    private GovernanceRuleRepository ruleRepository;
    private Set<MeshEnvListenerFactory> envListenerFactories;

    @BeforeEach
    public void setup() {
        originModule = ApplicationModel.defaultModel().getDefaultModule();
        moduleModel = Mockito.spy(originModule);

        ruleRepository = Mockito.mock(GovernanceRuleRepository.class);
        when(moduleModel.getDefaultExtension(GovernanceRuleRepository.class)).thenReturn(ruleRepository);

        ExtensionLoader<MeshEnvListenerFactory> envListenerFactoryLoader = Mockito.mock(ExtensionLoader.class);
        envListenerFactories = new HashSet<>();
        when(envListenerFactoryLoader.getSupportedExtensionInstances()).thenReturn(envListenerFactories);
        when(moduleModel.getExtensionLoader(MeshEnvListenerFactory.class)).thenReturn(envListenerFactoryLoader);
    }

    @AfterEach
    public void teardown() {
        originModule.destroy();
    }

    @Test
    public void testRegister1() {
        MeshRuleManager meshRuleManager = new MeshRuleManager(moduleModel);

        MeshRuleListener meshRuleListener1 = new MeshRuleListener() {
            @Override
            public void onRuleChange(String appName, List<Map<String, Object>> rules) {
                fail();
            }

            @Override
            public void clearRule(String appName) {

            }

            @Override
            public String ruleSuffix() {
                return "Type1";
            }
        };

        meshRuleManager.register("dubbo-demo", meshRuleListener1);

        assertEquals(1, meshRuleManager.getAppRuleListeners().size());
        verify(ruleRepository, times(1)).getRule("dubbo-demo.MESHAPPRULE", "dubbo", 5000L);

        MeshAppRuleListener meshAppRuleListener = meshRuleManager.getAppRuleListeners().values().iterator().next();
        verify(ruleRepository, times(1)).addListener("dubbo-demo.MESHAPPRULE", "dubbo",
            meshAppRuleListener);

        meshRuleManager.register("dubbo-demo", meshRuleListener1);
        assertEquals(1, meshRuleManager.getAppRuleListeners().size());

        MeshRuleListener meshRuleListener2 = new MeshRuleListener() {
            @Override
            public void onRuleChange(String appName, List<Map<String, Object>> rules) {
                fail();
            }

            @Override
            public void clearRule(String appName) {

            }

            @Override
            public String ruleSuffix() {
                return "Type2";
            }
        };
        meshRuleManager.register("dubbo-demo", meshRuleListener2);
        assertEquals(1, meshRuleManager.getAppRuleListeners().size());
        assertEquals(2, meshAppRuleListener.getMeshRuleDispatcher().getListenerMap().size());

        meshRuleManager.unregister("dubbo-demo", meshRuleListener1);
        assertEquals(1, meshRuleManager.getAppRuleListeners().size());
        assertEquals(1, meshAppRuleListener.getMeshRuleDispatcher().getListenerMap().size());

        meshRuleManager.unregister("dubbo-demo", meshRuleListener2);
        assertEquals(0, meshRuleManager.getAppRuleListeners().size());

        verify(ruleRepository, times(1)).removeListener("dubbo-demo.MESHAPPRULE", "dubbo",
            meshAppRuleListener);
    }

    @Test
    public void testRegister2() {
        MeshRuleManager meshRuleManager = new MeshRuleManager(moduleModel);

        AtomicInteger invokeTimes = new AtomicInteger(0);
        MeshRuleListener meshRuleListener = new MeshRuleListener() {
            @Override
            public void onRuleChange(String appName, List<Map<String, Object>> rules) {
                assertEquals("dubbo-demo", appName);
                Yaml yaml = new Yaml(new SafeConstructor());
                assertTrue(rules.contains(yaml.load(rule1)));
                assertTrue(rules.contains(yaml.load(rule2)));

                invokeTimes.incrementAndGet();
            }

            @Override
            public void clearRule(String appName) {

            }

            @Override
            public String ruleSuffix() {
                return "Type1";
            }
        };

        when(ruleRepository.getRule("dubbo-demo.MESHAPPRULE", "dubbo", 5000L)).thenReturn(rule1 + "---\n" + rule2);

        meshRuleManager.register("dubbo-demo", meshRuleListener);

        assertEquals(1, meshRuleManager.getAppRuleListeners().size());
        verify(ruleRepository, times(1)).getRule("dubbo-demo.MESHAPPRULE", "dubbo", 5000L);
        verify(ruleRepository, times(1)).addListener("dubbo-demo.MESHAPPRULE", "dubbo",
            meshRuleManager.getAppRuleListeners().values().iterator().next());
        assertEquals(1, invokeTimes.get());

        meshRuleManager.register("dubbo-demo", meshRuleListener);
        assertEquals(1, meshRuleManager.getAppRuleListeners().size());
    }

    @Test
    public void testRegister3() {
        MeshEnvListenerFactory meshEnvListenerFactory1 = Mockito.mock(MeshEnvListenerFactory.class);
        MeshEnvListenerFactory meshEnvListenerFactory2 = Mockito.mock(MeshEnvListenerFactory.class);

        MeshEnvListener meshEnvListener1 = Mockito.mock(MeshEnvListener.class);
        when(meshEnvListenerFactory1.getListener()).thenReturn(meshEnvListener1);
        MeshEnvListener meshEnvListener2 = Mockito.mock(MeshEnvListener.class);
        when(meshEnvListenerFactory2.getListener()).thenReturn(meshEnvListener2);

        envListenerFactories.add(meshEnvListenerFactory1);
        envListenerFactories.add(meshEnvListenerFactory2);

        MeshRuleManager meshRuleManager = new MeshRuleManager(moduleModel);

        MeshRuleListener meshRuleListener1 = new MeshRuleListener() {
            @Override
            public void onRuleChange(String appName, List<Map<String, Object>> rules) {
                fail();
            }

            @Override
            public void clearRule(String appName) {

            }

            @Override
            public String ruleSuffix() {
                return "Type1";
            }
        };

        when(meshEnvListener1.isEnable()).thenReturn(false);
        when(meshEnvListener2.isEnable()).thenReturn(true);

        meshRuleManager.register("dubbo-demo", meshRuleListener1);

        assertEquals(1, meshRuleManager.getAppRuleListeners().size());
        verify(ruleRepository, times(1)).getRule("dubbo-demo.MESHAPPRULE", "dubbo", 5000L);
        MeshAppRuleListener meshAppRuleListener = meshRuleManager.getAppRuleListeners().values().iterator().next();
        verify(ruleRepository, times(1)).addListener("dubbo-demo.MESHAPPRULE", "dubbo",
            meshAppRuleListener);

        verify(meshEnvListener2, times(1)).onSubscribe("dubbo-demo",
            meshAppRuleListener);

        meshRuleManager.register("dubbo-demo", meshRuleListener1);
        assertEquals(1, meshRuleManager.getAppRuleListeners().size());


        MeshRuleListener meshRuleListener2 = new MeshRuleListener() {
            @Override
            public void onRuleChange(String appName, List<Map<String, Object>> rules) {
                fail();
            }

            @Override
            public void clearRule(String appName) {

            }

            @Override
            public String ruleSuffix() {
                return "Type2";
            }
        };
        meshRuleManager.register("dubbo-demo", meshRuleListener2);
        assertEquals(1, meshRuleManager.getAppRuleListeners().size());
        assertEquals(2, meshAppRuleListener.getMeshRuleDispatcher().getListenerMap().size());

        meshRuleManager.unregister("dubbo-demo", meshRuleListener1);
        assertEquals(1, meshRuleManager.getAppRuleListeners().size());
        assertEquals(1, meshAppRuleListener.getMeshRuleDispatcher().getListenerMap().size());

        meshRuleManager.unregister("dubbo-demo", meshRuleListener2);
        assertEquals(0, meshRuleManager.getAppRuleListeners().size());

        verify(ruleRepository, times(1)).removeListener("dubbo-demo.MESHAPPRULE", "dubbo",
            meshAppRuleListener);
        verify(meshEnvListener2, times(1)).onUnSubscribe("dubbo-demo");
    }
}
