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
package org.apache.dubbo.registry.multiple;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.apache.dubbo.common.constants.CommonConstants.REVISION_KEY;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.EXPORTED_SERVICES_REVISION_PROPERTY_NAME;

public class MultipleServiceDiscoveryTest {

    private static String mockZkAddress = "zookeeper://mock-zk:2181?check=false";

    @Test
    public void testOnEvent() {
        try {
            String metadata_111 = "{\"app\":\"app1\",\"revision\":\"111\",\"services\":{"
                    + "\"org.apache.dubbo.demo.DemoService:dubbo\":{\"name\":\"org.apache.dubbo.demo.DemoService\",\"protocol\":\"dubbo\",\"path\":\"org.apache.dubbo.demo.DemoService\",\"params\":{\"side\":\"provider\",\"release\":\"\",\"methods\":\"sayHello,sayHelloAsync\",\"deprecated\":\"false\",\"dubbo\":\"2.0.2\",\"pid\":\"72723\",\"interface\":\"org.apache.dubbo.demo.DemoService\",\"service-name-mapping\":\"true\",\"timeout\":\"3000\",\"generic\":\"false\",\"metadata-type\":\"remote\",\"delay\":\"5000\",\"application\":\"app1\",\"dynamic\":\"true\",\"REGISTRY_CLUSTER\":\"registry1\",\"anyhost\":\"true\",\"timestamp\":\"1625800233446\"}}"
                    + "}}";
            MetadataInfo metadataInfo = JsonUtils.toJavaObject(metadata_111, MetadataInfo.class);
            ApplicationModel applicationModel = ApplicationModel.defaultModel();
            applicationModel.getApplicationConfigManager().setApplication(new ApplicationConfig("app2"));
            String multipleUrl = String.format(
                    "multiple://mock-registry:2181?reference-registry=%s&child.a1=%s&check=false",
                    mockZkAddress, mockZkAddress);
            URL url = URL.valueOf(multipleUrl);
            url.setScopeModel(applicationModel);
            MultipleServiceDiscovery multipleServiceDiscovery = new MultipleServiceDiscovery(url);
            Class<MultipleServiceDiscovery> msdClass = MultipleServiceDiscovery.class;
            Field serviceDiscoveriesField = msdClass.getDeclaredField("serviceDiscoveries");
            serviceDiscoveriesField.setAccessible(true);
            ServiceDiscovery mockServiceDiscovery = Mockito.mock(ServiceDiscovery.class);
            Mockito.when(mockServiceDiscovery.getRemoteMetadata(Mockito.anyString(), Mockito.anyList()))
                    .thenReturn(metadataInfo);
            Map<String, ServiceDiscovery> mockServiceDiscoveries = new HashMap<>();
            mockServiceDiscoveries.put("child.a1", mockServiceDiscovery);
            serviceDiscoveriesField.set(multipleServiceDiscovery, mockServiceDiscoveries);
            MultipleServiceDiscovery.MultiServiceInstancesChangedListener listener =
                    (MultipleServiceDiscovery.MultiServiceInstancesChangedListener)
                            multipleServiceDiscovery.createListener(Sets.newHashSet("app1"));
            multipleServiceDiscovery.addServiceInstancesChangedListener(listener);

            MultipleServiceDiscovery.SingleServiceInstancesChangedListener singleListener =
                    listener.getAndComputeIfAbsent("child.a1", (a1) -> null);
            Assert.notNull(singleListener, "singleServiceInstancesChangedListener can not be null");

            List<Object> urlsSameRevision = new ArrayList<>();
            urlsSameRevision.add("127.0.0.1:20880?revision=111");
            urlsSameRevision.add("127.0.0.2:20880?revision=111");
            urlsSameRevision.add("127.0.0.3:20880?revision=111");
            singleListener.onEvent(new ServiceInstancesChangedEvent("app1", buildInstances(urlsSameRevision)));
            Mockito.verify(mockServiceDiscovery, Mockito.times(1))
                    .getRemoteMetadata(Mockito.anyString(), Mockito.anyList());
            Field serviceUrlsField = ServiceInstancesChangedListener.class.getDeclaredField("serviceUrls");
            serviceUrlsField.setAccessible(true);
            Map<String, List<ServiceInstancesChangedListener.ProtocolServiceKeyWithUrls>> map =
                    (Map<String, List<ServiceInstancesChangedListener.ProtocolServiceKeyWithUrls>>)
                            serviceUrlsField.get(listener);
            Assert.assertTrue(!CollectionUtils.isEmptyMap(map), "url can not be empty");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    static List<ServiceInstance> buildInstances(List<Object> rawURls) {
        List<ServiceInstance> instances = new ArrayList<>();

        for (Object obj : rawURls) {
            String rawURL = (String) obj;
            DefaultServiceInstance instance = new DefaultServiceInstance();
            final URL dubboUrl = URL.valueOf(rawURL);
            instance.setRawAddress(rawURL);
            instance.setHost(dubboUrl.getHost());
            instance.setEnabled(true);
            instance.setHealthy(true);
            instance.setPort(dubboUrl.getPort());
            instance.setRegistryCluster("default");
            instance.setApplicationModel(ApplicationModel.defaultModel());

            Map<String, String> metadata = new HashMap<>();
            if (StringUtils.isNotEmpty(dubboUrl.getParameter(REVISION_KEY))) {
                metadata.put(EXPORTED_SERVICES_REVISION_PROPERTY_NAME, dubboUrl.getParameter(REVISION_KEY));
            }
            instance.setMetadata(metadata);

            instances.add(instance);
        }

        return instances;
    }
}
