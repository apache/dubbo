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
package org.apache.dubbo.registry.client;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.registry.ProviderFirstParams;
import org.apache.dubbo.rpc.RpcServiceContext;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TAG_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.in;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InstanceAddressURLTest {
    private static URL url = URL.valueOf("dubbo://30.225.21.30:20880/org.apache.dubbo.registry.service.DemoService?" +
        "REGISTRY_CLUSTER=registry1&anyhost=true&application=demo-provider2&delay=1000&deprecated=false&dubbo=2.0.2" +
        "&dynamic=true&generic=false&group=greeting&interface=org.apache.dubbo.registry.service.DemoService" +
        "&metadata-type=remote&methods=sayHello&sayHello.timeout=7000&a.timeout=7777&pid=66666&release=&revision=1.0.0&service-name-mapping=true" +
        "&side=provider&timeout=1000&timestamp=1629970909999&version=1.0.0&dubbo.tag=provider&params-filter=-default");

    private static URL url2 = URL.valueOf("dubbo://30.225.21.30:20880/org.apache.dubbo.registry.service.DemoService2?" +
        "REGISTRY_CLUSTER=registry1&anyhost=true&application=demo-provider2&delay=5000&deprecated=false&dubbo=2.0.2" +
        "&dynamic=true&generic=false&group=greeting&interface=org.apache.dubbo.registry.service.DemoService2" +
        "&metadata-type=remote&methods=sayHello&sayHello.timeout=7000&pid=36621&release=&revision=1.0.0&service-name-mapping=true" +
        "&side=provider&timeout=5000&timestamp=1629970068002&version=1.0.0&dubbo.tag=provider2&uniqueKey=unique&params-filter=-default");

    private static URL consumerURL = URL.valueOf("dubbo://30.225.21.30/org.apache.dubbo.registry.service.DemoService?" +
        "REGISTRY_CLUSTER=registry1&application=demo-consumer&dubbo=2.0.2" +
        "&group=greeting&interface=org.apache.dubbo.registry.service.DemoService" +
        "&version=1.0.0&timeout=9000&a.timeout=8888&dubbo.tag=consumer&protocol=dubbo");

    private DefaultServiceInstance createInstance() {
        DefaultServiceInstance instance = new DefaultServiceInstance("demo-provider", "127.0.0.1", 8080, ApplicationModel.defaultModel());
        Map<String, String> metadata = instance.getMetadata();
        metadata.put("key1", "value1");
        metadata.put("key2", "value2");
        return instance;
    }

    private MetadataInfo createMetaDataInfo() {
        MetadataInfo metadataInfo = new MetadataInfo("demo");
        // export normal url again
        metadataInfo.addService(url);
        metadataInfo.addService(url2);
        return metadataInfo;
    }

    private InstanceAddressURL instanceURL;
    private volatile transient Set<String> providerFirstParams;

    @BeforeEach
    public void setUp() {
        DefaultServiceInstance instance = createInstance();
        MetadataInfo metadataInfo = createMetaDataInfo();
        instanceURL = new InstanceAddressURL(instance, metadataInfo);
        Set<ProviderFirstParams> providerFirstParams = ApplicationModel.defaultModel().getExtensionLoader(ProviderFirstParams.class).getSupportedExtensionInstances();
        if (CollectionUtils.isEmpty(providerFirstParams)) {
            this.providerFirstParams = null;
        } else {
            if (providerFirstParams.size() == 1) {
                this.providerFirstParams = Collections.unmodifiableSet(providerFirstParams.iterator().next().params());
            } else {
                Set<String> params = new HashSet<>();
                for (ProviderFirstParams paramsFilter : providerFirstParams) {
                    if (paramsFilter.params() == null) {
                        break;
                    }
                    params.addAll(paramsFilter.params());
                }
                this.providerFirstParams = Collections.unmodifiableSet(params);
            }
        }
        instanceURL.setProviderFirstParams(this.providerFirstParams);
    }

    @Test
    public void test1() {
        // test reading of keys in instance and metadata work fine
        assertEquals("value1", instanceURL.getParameter("key1"));//return instance key
        assertNull(instanceURL.getParameter("delay"));// no service key specified
        RpcServiceContext.setRpcContext(consumerURL);
        assertEquals("1000", instanceURL.getParameter("delay"));
        assertEquals("1000", instanceURL.getServiceParameter(consumerURL.getProtocolServiceKey(), "delay"));
        assertEquals("9000", instanceURL.getMethodParameter("sayHello", "timeout"));
        assertEquals("9000", instanceURL.getServiceMethodParameter(consumerURL.getProtocolServiceKey(), "sayHello", "timeout"));
        assertNull(instanceURL.getParameter("uniqueKey"));
        assertNull(instanceURL.getServiceParameter(consumerURL.getProtocolServiceKey(), "uniqueKey"));
        assertEquals("unique", instanceURL.getServiceParameter(url2.getProtocolServiceKey(), "uniqueKey"));


        // test some consumer keys have higher priority
        assertEquals("8888", instanceURL.getServiceMethodParameter(consumerURL.getProtocolServiceKey(), "a", "timeout"));
        assertEquals("9000", instanceURL.getParameter("timeout"));

        // test some provider keys have higher priority
        assertEquals("provider", instanceURL.getParameter(TAG_KEY));

        assertEquals(instanceURL.getVersion(), instanceURL.getParameter(VERSION_KEY));
        assertEquals(instanceURL.getGroup(), instanceURL.getParameter(GROUP_KEY));
        assertEquals(instanceURL.getApplication(), instanceURL.getParameter(APPLICATION_KEY));
        assertEquals("demo-consumer", instanceURL.getParameter(APPLICATION_KEY));
        assertEquals(instanceURL.getRemoteApplication(), instanceURL.getParameter(REMOTE_APPLICATION_KEY));
        assertEquals("demo-provider", instanceURL.getParameter(REMOTE_APPLICATION_KEY));
        assertEquals(instanceURL.getSide(), instanceURL.getParameter(SIDE_KEY));
//        assertThat(Arrays.asList("7000", "8888"), hasItem(instanceURL.getAnyMethodParameter("timeout")));
        assertThat(instanceURL.getAnyMethodParameter("timeout"), in(Arrays.asList("7000", "8888")));
        Map<String, String> expectedAllParams = new HashMap<>();
        expectedAllParams.putAll(instanceURL.getInstance().getMetadata());
        expectedAllParams.putAll(instanceURL.getMetadataInfo().getServiceInfo(consumerURL.getProtocolServiceKey()).getAllParams());
        Map<String, String> consumerURLParameters = consumerURL.getParameters();
        providerFirstParams.forEach(consumerURLParameters::remove);
        expectedAllParams.putAll(consumerURLParameters);

        assertEquals(expectedAllParams.size(), instanceURL.getParameters().size());
        assertEquals(url.getParameter(TAG_KEY), instanceURL.getParameters().get(TAG_KEY));
        assertEquals(consumerURL.getParameter(TIMEOUT_KEY), instanceURL.getParameters().get(TIMEOUT_KEY));
        assertTrue(instanceURL.hasServiceMethodParameter(url.getProtocolServiceKey(), "a"));
        assertTrue(instanceURL.hasServiceMethodParameter(url.getProtocolServiceKey(), "sayHello"));
        assertTrue(instanceURL.hasMethodParameter("a", TIMEOUT_KEY));
        assertTrue(instanceURL.hasMethodParameter(null, TIMEOUT_KEY));
        assertEquals("8888", instanceURL.getMethodParameter("a", TIMEOUT_KEY));
        assertTrue(instanceURL.hasMethodParameter("a", null));
        assertFalse(instanceURL.hasMethodParameter("notExistMethod", null));

        // keys added to instance url are shared among services.
        instanceURL.addParameter("newKey", "newValue");
        assertEquals("newValue", instanceURL.getParameter("newKey"));
        assertEquals("newValue", instanceURL.getParameters().get("newKey"));
        assertEquals("newValue", instanceURL.getServiceParameters(url.getProtocolServiceKey()).get("newKey"));
    }

}
