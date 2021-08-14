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
package org.apache.dubbo.rpc.cluster.support.registry;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.cluster.ClusterInvoker;
import org.apache.dubbo.rpc.cluster.Directory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;

import static org.apache.dubbo.common.constants.CommonConstants.PREFERRED_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_ZONE;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_ZONE_FORCE;
import static org.apache.dubbo.common.constants.RegistryConstants.ZONE_KEY;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class ZoneAwareClusterInvokerTest {

    private Directory directory = mock(Directory.class);
    private ClusterInvoker firstInvoker = mock(ClusterInvoker.class);
    private ClusterInvoker secondInvoker = mock(ClusterInvoker.class);
    private ClusterInvoker thirdInvoker = mock(ClusterInvoker.class);
    private Invocation invocation = mock(Invocation.class);

    private ZoneAwareClusterInvoker<ZoneAwareClusterInvokerTest> zoneAwareClusterInvoker;

    private URL url = URL.valueOf("test://test");
    private URL registryUrl = URL.valueOf("localhost://test");

    String expectedValue = "expected";
    String unexpectedValue = "unexpected";

    @Test
    public void testPreferredStrategy() {
        given(invocation.getParameterTypes()).willReturn(new Class<?>[]{});
        given(invocation.getArguments()).willReturn(new Object[]{});
        given(invocation.getObjectAttachments()).willReturn(new HashMap<>());

        firstInvoker = newUnexpectedInvoker();
        thirdInvoker = newUnexpectedInvoker();

        secondInvoker = (ClusterInvoker) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ClusterInvoker.class}, (proxy, method, args) -> {
            if ("getUrl".equals(method.getName())) {
                return url;
            }
            if ("getRegistryUrl".equals(method.getName())) {
                return registryUrl.addParameter(PREFERRED_KEY, true);
            }
            if ("isAvailable".equals(method.getName())) {
                return true;
            }
            if ("invoke".equals(method.getName())) {
                return new AppResponse(expectedValue);
            }
            return null;
        });

        given(directory.list(invocation)).willReturn(new ArrayList() {
            {
                add(firstInvoker);
                add(secondInvoker);
                add(thirdInvoker);
            }
        });

        given(directory.getUrl()).willReturn(url);
        given(directory.getConsumerUrl()).willReturn(url);

        zoneAwareClusterInvoker = new ZoneAwareClusterInvoker<>(directory);
        AppResponse response = (AppResponse) zoneAwareClusterInvoker.invoke(invocation);
        Assertions.assertEquals(expectedValue, response.getValue());
    }

    @Test
    public void testRegistryZoneStrategy() {
        String zoneKey = "zone";

        given(invocation.getParameterTypes()).willReturn(new Class<?>[]{});
        given(invocation.getArguments()).willReturn(new Object[]{});
        given(invocation.getObjectAttachments()).willReturn(new HashMap<>());
        given(invocation.getAttachment(REGISTRY_ZONE)).willReturn(zoneKey);

        firstInvoker = newUnexpectedInvoker();
        thirdInvoker = newUnexpectedInvoker();

        secondInvoker = (ClusterInvoker) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ClusterInvoker.class}, (proxy, method, args) -> {
            if ("getUrl".equals(method.getName())) {
                return url;
            }
            if ("getRegistryUrl".equals(method.getName())) {
                return registryUrl.addParameter(ZONE_KEY, zoneKey);
            }
            if ("isAvailable".equals(method.getName())) {
                return true;
            }
            if ("invoke".equals(method.getName())) {
                return new AppResponse(expectedValue);
            }
            return null;
        });

        given(directory.list(invocation)).willReturn(new ArrayList() {
            {
                add(firstInvoker);
                add(secondInvoker);
                add(thirdInvoker);
            }
        });

        given(directory.getUrl()).willReturn(url);
        given(directory.getConsumerUrl()).willReturn(url);

        zoneAwareClusterInvoker = new ZoneAwareClusterInvoker<>(directory);
        AppResponse response = (AppResponse) zoneAwareClusterInvoker.invoke(invocation);
        Assertions.assertEquals(expectedValue, response.getValue());
    }

    @Test
    public void testRegistryZoneForceStrategy() {
        String zoneKey = "zone";

        given(invocation.getParameterTypes()).willReturn(new Class<?>[]{});
        given(invocation.getArguments()).willReturn(new Object[]{});
        given(invocation.getObjectAttachments()).willReturn(new HashMap<>());
        given(invocation.getAttachment(REGISTRY_ZONE)).willReturn(zoneKey);
        given(invocation.getAttachment(REGISTRY_ZONE_FORCE)).willReturn("true");

        firstInvoker = newUnexpectedInvoker();
        secondInvoker = newUnexpectedInvoker();
        thirdInvoker = newUnexpectedInvoker();

        given(directory.list(invocation)).willReturn(new ArrayList() {
            {
                add(firstInvoker);
                add(secondInvoker);
                add(thirdInvoker);
            }
        });

        given(directory.getUrl()).willReturn(url);
        given(directory.getConsumerUrl()).willReturn(url);

        zoneAwareClusterInvoker = new ZoneAwareClusterInvoker<>(directory);
        Assertions.assertThrows(IllegalStateException.class,
            () -> zoneAwareClusterInvoker.invoke(invocation));
    }

    private ClusterInvoker newUnexpectedInvoker() {
        return  (ClusterInvoker) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ClusterInvoker.class}, (proxy, method, args) -> {
            if ("getUrl".equals(method.getName())) {
                return url;
            }
            if ("getRegistryUrl".equals(method.getName())) {
                return registryUrl;
            }
            if ("isAvailable".equals(method.getName())) {
                return true;
            }
            if ("invoke".equals(method.getName())) {
                return new AppResponse(unexpectedValue);
            }
            return null;
        });
    }
}
