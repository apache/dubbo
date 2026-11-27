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
package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.PathResolver;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.ServiceModel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TriplePathResolverTest {
    private boolean originalFallback;

    private static final PathResolver PATH_RESOLVER =
            FrameworkModel.defaultModel().getDefaultExtension(PathResolver.class);

    private static final String SERVICE_NAME = "DemoService";

    private static final Invoker<Object> INVOKER = new Invoker<Object>() {
        @Override
        public URL getUrl() {
            ServiceDescriptor serviceDescriptor = Mockito.mock(ServiceDescriptor.class);
            Mockito.when(serviceDescriptor.getInterfaceName()).thenReturn(SERVICE_NAME);
            ServiceModel serviceModel = Mockito.mock(ServiceModel.class);
            Mockito.when(serviceModel.getServiceModel()).thenReturn(serviceDescriptor);
            return URL.valueOf("tri://localhost/demo/" + SERVICE_NAME)
                    .setServiceInterface(SERVICE_NAME)
                    .addParameter(CommonConstants.GROUP_KEY, "g1")
                    .addParameter(CommonConstants.VERSION_KEY, "1.0.1")
                    .setServiceModel(serviceModel);
        }

        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        public void destroy() {}

        @Override
        public Class<Object> getInterface() {
            return null;
        }

        @Override
        public Result invoke(Invocation invocation) throws RpcException {
            return null;
        }
    };

    @BeforeEach
    public void init() {
        originalFallback = TripleProtocol.RESOLVE_FALLBACK_TO_DEFAULT;
        PATH_RESOLVER.add("/abc", INVOKER);
        PATH_RESOLVER.register(INVOKER);
    }

    @AfterEach
    public void destroy() {
        PATH_RESOLVER.destroy();
        TripleProtocol.RESOLVE_FALLBACK_TO_DEFAULT = originalFallback;
    }

    @Test
    void testResolve() {
        Assertions.assertEquals(INVOKER, getInvokerByPath("/abc"));
    }

    @Test
    void testResolveWithContextPath() {
        String path = "demo/" + SERVICE_NAME;
        Assertions.assertEquals(INVOKER, PATH_RESOLVER.resolve(SERVICE_NAME, null, null));
        Assertions.assertEquals(INVOKER, PATH_RESOLVER.resolve(path, null, null));
        Assertions.assertEquals(INVOKER, PATH_RESOLVER.resolve(SERVICE_NAME, "g1", "1.0.1"));
        Assertions.assertEquals(INVOKER, PATH_RESOLVER.resolve(path, "g1", "1.0.1"));
        Assertions.assertEquals(INVOKER, PATH_RESOLVER.resolve(SERVICE_NAME, "g2", "1.0.2"));
        Assertions.assertEquals(INVOKER, PATH_RESOLVER.resolve(path, "g2", "1.0.2"));
        TripleProtocol.RESOLVE_FALLBACK_TO_DEFAULT = false;
        Assertions.assertEquals(INVOKER, PATH_RESOLVER.resolve(SERVICE_NAME, "g1", "1.0.1"));
        Assertions.assertEquals(INVOKER, PATH_RESOLVER.resolve(path, "g1", "1.0.1"));
        Assertions.assertNull(PATH_RESOLVER.resolve(SERVICE_NAME, "g2", "1.0.2"));
        Assertions.assertNull(PATH_RESOLVER.resolve(path, "g2", "1.0.2"));
    }

    @Test
    void testRemove() {
        Assertions.assertEquals(INVOKER, getInvokerByPath("/abc"));
        PATH_RESOLVER.remove("/abc");
        Assertions.assertNull(getInvokerByPath("/abc"));
    }

    @Test
    void testNative() {
        String path = "path";
        Assertions.assertFalse(PATH_RESOLVER.hasNativeStub(path));
        PATH_RESOLVER.addNativeStub(path);
        Assertions.assertTrue(PATH_RESOLVER.hasNativeStub(path));
    }

    @Test
    void testDestroy() {
        Assertions.assertEquals(INVOKER, getInvokerByPath("/abc"));
        {
            PATH_RESOLVER.add("/bcd", INVOKER);
            Assertions.assertEquals(INVOKER, getInvokerByPath("/bcd"));
        }
        PATH_RESOLVER.destroy();
        Assertions.assertNull(getInvokerByPath("/abc"));
        Assertions.assertNull(getInvokerByPath("/bcd"));
    }

    @Test
    void protoAliasesKeepExactGroupAndVersionAndUnregisterOnlyTheirOwner() {
        TriplePathResolver resolver = new TriplePathResolver();
        Invoker<?> first = alias("java.WireEcho", "proto.WireEcho", "proto.WireEcho", "blue", "1.2.0");
        Invoker<?> nextVersion = alias("java.WireEcho", "proto.WireEcho", "proto.WireEcho", "blue", "2.0.0");
        Invoker<?> otherGroup = alias("java.WireEcho", "proto.WireEcho", "proto.WireEcho", "green", "2.0.0");
        resolver.register(first);
        resolver.register(nextVersion);
        resolver.register(otherGroup);
        Assertions.assertSame(first, resolver.resolve("proto.WireEcho", "blue", "1.2.0"));
        Assertions.assertSame(nextVersion, resolver.resolve("proto.WireEcho", "blue", "2.0.0"));
        Assertions.assertSame(otherGroup, resolver.resolve("proto.WireEcho", "green", "2.0.0"));
        Assertions.assertSame(otherGroup, resolver.resolve("java.WireEcho", "green", "2.0.0"));
        resolver.unregister(otherGroup);
        Assertions.assertSame(first, resolver.resolve("proto.WireEcho", null, null));
        TripleProtocol.RESOLVE_FALLBACK_TO_DEFAULT = false;
        Assertions.assertNull(resolver.resolve("proto.WireEcho", "green", "2.0.0"));
        Assertions.assertSame(first, resolver.resolve("proto.WireEcho", "blue", "1.2.0"));
        Assertions.assertSame(nextVersion, resolver.resolve("proto.WireEcho", "blue", "2.0.0"));
        resolver.unregister(first);
        Assertions.assertSame(nextVersion, resolver.resolve("proto.WireEcho", "blue", "2.0.0"));
        resolver.unregister(nextVersion);
        Assertions.assertNull(resolver.resolve("proto.WireEcho", "blue", "2.0.0"));
        Assertions.assertNull(resolver.resolve("java.WireEcho", "blue", "2.0.0"));
    }

    @Test
    void customWirePathsAndContextFallbacksAreRemovedWhenUnexported() {
        for (String path : new String[] {"custom.Wire", "ctx/custom.Wire"}) {
            TriplePathResolver resolver = new TriplePathResolver();
            Invoker<?> invoker = alias("java.Pojo", "java.Pojo", path, "group", "1.2.0");
            resolver.register(invoker);
            Assertions.assertSame(invoker, resolver.resolve(path, "group", "1.2.0"));
            Assertions.assertSame(invoker, resolver.resolve("java.Pojo", "group", "1.2.0"));
            resolver.unregister(invoker);
            Assertions.assertNull(resolver.resolve(path, "group", "1.2.0"));
            Assertions.assertNull(resolver.resolve("java.Pojo", null, null));
            Assertions.assertNull(resolver.resolve("ctx/java.Pojo", null, null));
        }
    }

    private static Invoker<?> alias(String javaName, String descriptorName, String path, String group, String version) {
        ServiceDescriptor descriptor = Mockito.mock(ServiceDescriptor.class);
        Mockito.when(descriptor.getInterfaceName()).thenReturn(descriptorName);
        ServiceModel model = Mockito.mock(ServiceModel.class);
        Mockito.when(model.getServiceModel()).thenReturn(descriptor);
        URL url = URL.valueOf("tri://localhost/" + path)
                .setServiceInterface(javaName)
                .addParameter("group", group)
                .addParameter("version", version)
                .setServiceModel(model);
        Invoker<?> invoker = Mockito.mock(Invoker.class);
        Mockito.when(invoker.getUrl()).thenReturn(url);
        return invoker;
    }

    private Invoker<?> getInvokerByPath(String path) {
        return PATH_RESOLVER.resolve(path, null, null);
    }
}
