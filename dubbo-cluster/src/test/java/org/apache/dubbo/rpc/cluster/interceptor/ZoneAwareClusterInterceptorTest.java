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
package org.apache.dubbo.rpc.cluster.interceptor;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.ZoneDetector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.apache.dubbo.rpc.cluster.interceptor.ZoneAwareClusterInterceptor.NAME;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_ZONE;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_ZONE_FORCE;

public class ZoneAwareClusterInterceptorTest {

    private ZoneAwareClusterInterceptor interceptor;

    @BeforeEach
    public void setUp() {
        interceptor = (ZoneAwareClusterInterceptor) ExtensionLoader.
                getExtensionLoader(ClusterInterceptor.class).getExtension(NAME);
    }

    @Test
    public void testGetExtension() {
        Assertions.assertNotNull(interceptor);
        Assertions.assertTrue(interceptor instanceof ZoneAwareClusterInterceptor);
    }

    @Test
    public void testGetActivateExtension() {
        URL url = URL.valueOf("test://test:80?cluster=zone-aware");
        List<ClusterInterceptor> interceptors = ExtensionLoader.
                getExtensionLoader(ClusterInterceptor.class).getActivateExtension(url, "cluster");

        Assertions.assertNotNull(interceptors);
        long count = interceptors.stream().
                filter(interceptor -> interceptor instanceof ZoneAwareClusterInterceptor).count();

        Assertions.assertEquals(1L, count);
    }

    @Test
    public void testBefore() {
        RpcInvocation invocation = new RpcInvocation();
        interceptor.before(null, invocation);
        Assertions.assertEquals("mock_zone", invocation.getAttachment(REGISTRY_ZONE));
        Assertions.assertEquals("mock_force", invocation.getAttachment(REGISTRY_ZONE_FORCE));

        RpcContext context = RpcContext.getContext();
        context.setAttachment(REGISTRY_ZONE, "context_zone");
        context.setAttachment(REGISTRY_ZONE_FORCE, "context_force");
        interceptor.before(null, invocation);
        Assertions.assertEquals("context_zone", invocation.getAttachment(REGISTRY_ZONE));
        Assertions.assertEquals("context_force", invocation.getAttachment(REGISTRY_ZONE_FORCE));
    }

    @Test
    public void testAfter() {
        // pass
        interceptor.after(null, null);
    }

    public static class MockZoneDetector implements ZoneDetector {

        @Override
        public String getZoneOfCurrentRequest(Invocation invocation) {
            return "mock_zone";
        }

        @Override
        public String isZoneForcingEnabled(Invocation invocation, String zone) {
            return "mock_force";
        }
    }
}
