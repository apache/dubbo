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
package org.apache.dubbo.registry.integration;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.deploy.ApplicationDeployer;
import org.apache.dubbo.common.status.reporter.FrameworkStatusReportService;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.apache.dubbo.common.status.reporter.FrameworkStatusReportService.REGISTRATION_STATUS;

/**
 * Covers the dual-registration failure-isolation path introduced in
 * {@link RegistryProtocol#registerWithModeTag(Registry, URL, URL)}:
 * <ul>
 *   <li>success case reports SUCCESS tagged with INTERFACE_REGISTER / INSTANCE_REGISTER</li>
 *   <li>check=true rethrows but still reports FAILED with the mode tag</li>
 *   <li>check=false swallows the failure, reports FAILED, and returns false</li>
 * </ul>
 */
class RegistryProtocolDualRegisterTest {

    private FrameworkModel frameworkModel;
    private ApplicationModel applicationModel;

    @BeforeEach
    void setUp() {
        CapturingFrameworkStatusReporter.clear();
        frameworkModel = new FrameworkModel();
        applicationModel = frameworkModel.newApplication();
        ApplicationConfig app = new ApplicationConfig("APP");
        applicationModel.getApplicationConfigManager().setApplication(app);
        // Ensure FrameworkStatusReportService is initialized and wires up reporters
        applicationModel.getBeanFactory().getBean(FrameworkStatusReportService.class);

        // registerWithModeTag calls register() which touches ApplicationDeployer
        ApplicationDeployer deployer = Mockito.mock(ApplicationDeployer.class);
        applicationModel.setDeployer(deployer);
    }

    @AfterEach
    void tearDown() {
        frameworkModel.destroy();
        CapturingFrameworkStatusReporter.clear();
    }

    @Test
    void successReportsInterfaceRegister() throws Exception {
        URL registryUrl = registryUrl("zookeeper", false);
        URL providerUrl = providerUrl();
        Registry registry = Mockito.mock(Registry.class);
        Mockito.when(registry.getUrl()).thenReturn(registryUrl);

        boolean result = invokeRegisterWithModeTag(registry, registryUrl, providerUrl);

        Assertions.assertTrue(result, "registerWithModeTag must return true on success");
        Mockito.verify(registry).register(providerUrl);
        Map<String, String> payload = latestRegistrationPayload();
        Assertions.assertEquals("INTERFACE_REGISTER", payload.get("mode"));
        Assertions.assertEquals("SUCCESS", payload.get("status"));
        Assertions.assertNull(payload.get("error"));
    }

    @Test
    void successReportsInstanceRegisterForServiceDiscoveryProtocol() throws Exception {
        URL registryUrl = registryUrl("service-discovery-registry", false);
        URL providerUrl = providerUrl();
        Registry registry = Mockito.mock(Registry.class);
        Mockito.when(registry.getUrl()).thenReturn(registryUrl);

        boolean result = invokeRegisterWithModeTag(registry, registryUrl, providerUrl);

        Assertions.assertTrue(result);
        Assertions.assertEquals("INSTANCE_REGISTER", latestRegistrationPayload().get("mode"));
    }

    @Test
    void checkTrueRethrowsButStillReportsFailure() {
        URL registryUrl = registryUrl("zookeeper", true);
        URL providerUrl = providerUrl();
        Registry registry = Mockito.mock(Registry.class);
        Mockito.when(registry.getUrl()).thenReturn(registryUrl);
        Mockito.doThrow(new IllegalStateException("NoNode for /dubbo/..."))
                .when(registry)
                .register(providerUrl);

        IllegalStateException thrown = Assertions.assertThrows(
                IllegalStateException.class, () -> invokeRegisterWithModeTag(registry, registryUrl, providerUrl));
        Assertions.assertEquals("NoNode for /dubbo/...", thrown.getMessage());

        Map<String, String> payload = latestRegistrationPayload();
        Assertions.assertEquals("INTERFACE_REGISTER", payload.get("mode"));
        Assertions.assertEquals("FAILED", payload.get("status"));
        Assertions.assertEquals("NoNode for /dubbo/...", payload.get("error"));
    }

    @Test
    void checkFalseSwallowsFailureAndReturnsFalse() throws Exception {
        URL registryUrl = registryUrl("service-discovery-registry", false);
        URL providerUrl = providerUrl();
        Registry registry = Mockito.mock(Registry.class);
        Mockito.when(registry.getUrl()).thenReturn(registryUrl);
        Mockito.doThrow(new IllegalStateException("metadata center unreachable"))
                .when(registry)
                .register(providerUrl);

        boolean result = invokeRegisterWithModeTag(registry, registryUrl, providerUrl);

        Assertions.assertFalse(result, "registerWithModeTag must swallow and return false when check=false");
        Map<String, String> payload = latestRegistrationPayload();
        Assertions.assertEquals("INSTANCE_REGISTER", payload.get("mode"));
        Assertions.assertEquals("FAILED", payload.get("status"));
        Assertions.assertEquals("metadata center unreachable", payload.get("error"));
    }

    private URL registryUrl(String protocol, boolean check) {
        Map<String, String> params = new HashMap<>();
        params.put("check", Boolean.toString(check));
        URL url = new ServiceConfigURL(protocol, "127.0.0.1", 2181, params);
        return url.setScopeModel(applicationModel);
    }

    private URL providerUrl() {
        URL url = new ServiceConfigURL(
                "dubbo", "127.0.0.1", 20880, "org.apache.dubbo.registry.integration.DemoService", new HashMap<>());
        return url.setScopeModel(applicationModel);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> latestRegistrationPayload() {
        Object raw = CapturingFrameworkStatusReporter.last(REGISTRATION_STATUS);
        Assertions.assertNotNull(raw, "registration outcome must be reported");
        return JsonUtils.toJavaObject(String.valueOf(raw), Map.class);
    }

    private boolean invokeRegisterWithModeTag(Registry registry, URL registryUrl, URL providerUrl) throws Exception {
        Method m =
                RegistryProtocol.class.getDeclaredMethod("registerWithModeTag", Registry.class, URL.class, URL.class);
        m.setAccessible(true);
        try {
            return (boolean) m.invoke(null, registry, registryUrl, providerUrl);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw e;
        }
    }
}
