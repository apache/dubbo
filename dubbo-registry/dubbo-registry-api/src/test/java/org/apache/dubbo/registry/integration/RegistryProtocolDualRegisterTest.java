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
import org.apache.dubbo.common.status.reporter.FrameworkStatusReporter;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.support.FailbackRegistry;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.apache.dubbo.common.status.reporter.FrameworkStatusReportService.REGISTRATION_STATUS;

/**
 * Covers the dual-registration failure-isolation path and outcome propagation in
 * {@link RegistryProtocol#registerWithModeTag(Registry, URL, URL)}:
 * <ul>
 *   <li>SUCCESS case reports SUCCESS tagged with INTERFACE_REGISTER / INSTANCE_REGISTER</li>
 *   <li>check=true rethrows but still reports FAILED with the mode tag</li>
 *   <li>check=false swallows the failure, reports FAILED, and returns FAILED</li>
 *   <li>FailbackRegistry silent-retry path (check=false, doRegister threw, URL queued for retry)
 *       reports PENDING_RETRY and returns PENDING_RETRY so caller-side state stays un-registered</li>
 * </ul>
 */
class RegistryProtocolDualRegisterTest {

    private FrameworkModel frameworkModel;
    private ApplicationModel applicationModel;

    @BeforeEach
    void setUp() throws Exception {
        CapturingFrameworkStatusReporter.clear();
        frameworkModel = new FrameworkModel();
        applicationModel = frameworkModel.newApplication();
        ApplicationConfig app = new ApplicationConfig("APP");
        applicationModel.getApplicationConfigManager().setApplication(app);

        // Register CapturingFrameworkStatusReporter programmatically. We avoid shipping an SPI file
        // in test resources because that would leak the reporter into every other test in this
        // module (e.g. MigrationRuleHandlerTest, whose URL has no ApplicationConfig and whose
        // migration path would then trigger applicationModel.getApplicationName() → ISE).
        applicationModel
                .getExtensionLoader(FrameworkStatusReporter.class)
                .addExtension("capturing", CapturingFrameworkStatusReporter.class);

        // FrameworkStatusReportService snapshots its `reporters` set during setApplicationModel,
        // which may have fired already (e.g. by ApplicationModel init) before our addExtension
        // above. Reload its reporters field from the extension loader so the capturer is present.
        FrameworkStatusReportService svc =
                applicationModel.getBeanFactory().getBean(FrameworkStatusReportService.class);
        java.lang.reflect.Field reportersField = FrameworkStatusReportService.class.getDeclaredField("reporters");
        reportersField.setAccessible(true);
        reportersField.set(
                svc,
                applicationModel
                        .getExtensionLoader(FrameworkStatusReporter.class)
                        .getSupportedExtensionInstances());

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

        String outcome = invokeRegisterWithModeTag(registry, registryUrl, providerUrl);

        Assertions.assertEquals("SUCCESS", outcome);
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

        String outcome = invokeRegisterWithModeTag(registry, registryUrl, providerUrl);

        Assertions.assertEquals("SUCCESS", outcome);
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
    void checkFalseSwallowsFailureAndReturnsFailed() throws Exception {
        URL registryUrl = registryUrl("service-discovery-registry", false);
        URL providerUrl = providerUrl();
        Registry registry = Mockito.mock(Registry.class);
        Mockito.when(registry.getUrl()).thenReturn(registryUrl);
        Mockito.doThrow(new IllegalStateException("metadata center unreachable"))
                .when(registry)
                .register(providerUrl);

        String outcome = invokeRegisterWithModeTag(registry, registryUrl, providerUrl);

        Assertions.assertEquals(
                "FAILED",
                outcome,
                "registerWithModeTag must swallow and return FAILED when check=false on a directly-throwing Registry");
        Map<String, String> payload = latestRegistrationPayload();
        Assertions.assertEquals("INSTANCE_REGISTER", payload.get("mode"));
        Assertions.assertEquals("FAILED", payload.get("status"));
        Assertions.assertEquals("metadata center unreachable", payload.get("error"));
    }

    /**
     * FailbackRegistry silent-retry path: {@code doRegister} throws, {@code check=false}, so the
     * exception is swallowed and the URL is queued for retry. {@code register()} returns normally.
     * The dual-register path must NOT classify this as SUCCESS — observers and caller-side state
     * need to see it as PENDING_RETRY so they don't prematurely treat the URL as published.
     */
    @Test
    void failbackSilentRetryReportsPendingRetry() throws Exception {
        URL registryUrl = registryUrl("zookeeper", false);
        URL providerUrl = providerUrl();
        FailingFailbackRegistry registry = new FailingFailbackRegistry(registryUrl);

        String outcome = invokeRegisterWithModeTag(registry, registryUrl, providerUrl);

        Assertions.assertEquals("PENDING_RETRY", outcome);
        Assertions.assertTrue(
                registry.isPendingFailedRegistration(providerUrl),
                "the URL should now be sitting in FailbackRegistry's retry queue");
        Map<String, String> payload = latestRegistrationPayload();
        Assertions.assertEquals("INTERFACE_REGISTER", payload.get("mode"));
        Assertions.assertEquals("PENDING_RETRY", payload.get("status"));
        Assertions.assertNotNull(
                payload.get("error"), "pending-retry payload should describe why the initial attempt failed");
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

    /**
     * Invokes the private static {@code registerWithModeTag} and returns the enum name of the
     * resulting {@code RegisterOutcome}, so test assertions can string-compare regardless of the
     * enum's package-private visibility.
     */
    private String invokeRegisterWithModeTag(Registry registry, URL registryUrl, URL providerUrl) throws Exception {
        Method m =
                RegistryProtocol.class.getDeclaredMethod("registerWithModeTag", Registry.class, URL.class, URL.class);
        m.setAccessible(true);
        try {
            Object outcome = m.invoke(null, registry, registryUrl, providerUrl);
            return ((Enum<?>) outcome).name();
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw e;
        }
    }

    /**
     * Minimal FailbackRegistry whose {@link #doRegister(URL)} always throws. Drives the real
     * {@code FailbackRegistry.register(URL)} body so {@link FailbackRegistry#isPendingFailedRegistration}
     * actually reflects the silent-retry queue, exactly like ZookeeperRegistry / NacosRegistry do
     * in production.
     */
    private static final class FailingFailbackRegistry extends FailbackRegistry {
        private final RuntimeException failure;

        FailingFailbackRegistry(URL registryUrl) {
            super(registryUrl);
            this.failure = new IllegalStateException("doRegister failed: ZK session expired");
        }

        @Override
        public void doRegister(URL url) {
            throw failure;
        }

        @Override
        public void doUnregister(URL url) {}

        @Override
        public void doSubscribe(URL url, NotifyListener listener) {}

        @Override
        public void doUnsubscribe(URL url, NotifyListener listener) {}

        @Override
        public boolean isAvailable() {
            return true;
        }

        // The following overrides keep the retry timer from firing during the short test window.
        @SuppressWarnings("unused")
        public List<URL> lookup(URL url) {
            return java.util.Collections.emptyList();
        }
    }

    /**
     * ExporterChangeableWrapper.register() must leave {@code registered=false} when the underlying
     * registration outcome is not SUCCESS. Otherwise a later manual register/re-register call is a
     * no-op and the wrapper permanently believes it is live. This test covers the PENDING_RETRY
     * branch (FailbackRegistry silent-retry under check=false).
     */
    @Test
    void exporterWrapperRegisterResetsFlagOnPendingRetry() throws Exception {
        URL registryUrl = registryUrl("zookeeper", false);
        URL providerUrl = providerUrl();
        FailingFailbackRegistry registry = new FailingFailbackRegistry(registryUrl);

        Object wrapper = newWrapperWith(registry, registryUrl, providerUrl);
        invokeWrapperRegister(wrapper);

        Assertions.assertFalse(
                readAtomicRegistered(wrapper),
                "wrapper.registered must be reset to false when registration is PENDING_RETRY");
    }

    /**
     * When {@code check=true} and {@code registry.register} throws, the wrapper must rethrow AND
     * reset its CAS flag. Without the reset, a follow-up register() call short-circuits on the CAS
     * and silently does nothing.
     */
    @Test
    void exporterWrapperRegisterResetsFlagOnCheckTrueException() throws Exception {
        URL registryUrl = registryUrl("zookeeper", true);
        URL providerUrl = providerUrl();
        Registry throwing = Mockito.mock(Registry.class);
        Mockito.when(throwing.getUrl()).thenReturn(registryUrl);
        Mockito.doThrow(new IllegalStateException("NoNode for /dubbo/..."))
                .when(throwing)
                .register(providerUrl);

        Object wrapper = newWrapperWith(throwing, registryUrl, providerUrl);
        Assertions.assertThrows(IllegalStateException.class, () -> invokeWrapperRegister(wrapper));
        Assertions.assertFalse(
                readAtomicRegistered(wrapper), "wrapper.registered must be reset to false after check=true rethrow");
    }

    /**
     * Constructs an {@code ExporterChangeableWrapper} via reflection and installs an outer
     * {@code RegistryProtocol} spy whose {@code getRegistry(URL)} returns the supplied fake. This
     * lets us exercise the wrapper's {@code register()} body without standing up the full
     * ReferenceCountExporter / RegistryFactory machinery.
     */
    private Object newWrapperWith(Registry registry, URL registryUrl, URL providerUrl) throws Exception {
        RegistryProtocol outer = new RegistryProtocol() {
            @Override
            protected Registry getRegistry(URL url) {
                return registry;
            }

            @Override
            protected URL getRegistryUrl(Invoker<?> originInvoker) {
                return registryUrl;
            }
        };
        java.lang.reflect.Field fmField = RegistryProtocol.class.getDeclaredField("frameworkModel");
        fmField.setAccessible(true);
        fmField.set(outer, frameworkModel);

        Invoker<?> originInvoker = Mockito.mock(Invoker.class);
        Mockito.when(originInvoker.getUrl()).thenReturn(providerUrl);

        Exporter<?> innerExporter = Mockito.mock(Exporter.class);
        ReferenceCountExporter<?> refExporter =
                new ReferenceCountExporter<>(innerExporter, providerUrl.getServiceKey(), null);

        Class<?> wrapperCls =
                Class.forName("org.apache.dubbo.registry.integration.RegistryProtocol$ExporterChangeableWrapper");
        java.lang.reflect.Constructor<?> ctor =
                wrapperCls.getDeclaredConstructor(RegistryProtocol.class, ReferenceCountExporter.class, Invoker.class);
        ctor.setAccessible(true);
        Object wrapper = ctor.newInstance(outer, refExporter, originInvoker);

        // The wrapper's register() reads getRegisterUrl() — stub it by setting the field directly.
        java.lang.reflect.Field registerUrlField = wrapperCls.getDeclaredField("registerUrl");
        registerUrlField.setAccessible(true);
        registerUrlField.set(wrapper, providerUrl);

        return wrapper;
    }

    private void invokeWrapperRegister(Object wrapper) throws Exception {
        java.lang.reflect.Method m = wrapper.getClass().getDeclaredMethod("register");
        m.setAccessible(true);
        try {
            m.invoke(wrapper);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw e;
        }
    }

    private boolean readAtomicRegistered(Object wrapper) throws Exception {
        java.lang.reflect.Field f = wrapper.getClass().getDeclaredField("registered");
        f.setAccessible(true);
        java.util.concurrent.atomic.AtomicBoolean ab = (java.util.concurrent.atomic.AtomicBoolean) f.get(wrapper);
        return ab.get();
    }
}
