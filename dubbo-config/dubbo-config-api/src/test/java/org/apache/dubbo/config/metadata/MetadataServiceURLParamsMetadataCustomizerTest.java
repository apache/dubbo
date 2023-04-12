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
package org.apache.dubbo.config.metadata;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.api.DemoService;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.provider.impl.DemoServiceImpl;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PORT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMESTAMP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.METADATA_SERVICE_URL_PARAMS_PROPERTY_NAME;

class MetadataServiceURLParamsMetadataCustomizerTest {

    public DefaultServiceInstance instance;
    private URL metadataServiceURL = URL.valueOf("dubbo://10.225.12.124:2002/org.apache.dubbo.metadata.MetadataService" +
        "?application=MetadataServiceURLParamsMetadataCustomizerTest&group=MetadataServiceURLParamsMetadataCustomizerTest" +
        "&interface=org.apache.dubbo.metadata.MetadataService&side=provider&timestamp=1637573430740&version=1.0.0");


    public static DefaultServiceInstance createInstance() {
        return new DefaultServiceInstance("A", "127.0.0.1", 20880, ApplicationModel.defaultModel());
    }

    @BeforeEach
    public void init() {
        instance = createInstance();
    }

    @AfterEach
    public void tearDown() throws IOException {
        Mockito.framework().clearInlineMocks();
    }

    @Test
    void test() {
        DubboBootstrap providerBootstrap = DubboBootstrap.newInstance();
        ServiceConfig<DemoService> serviceConfig = new ServiceConfig<>();
        serviceConfig.setInterface(DemoService.class);
        serviceConfig.setRef(new DemoServiceImpl());
        serviceConfig.setDelay(1000);

        ApplicationConfig applicationConfig = new ApplicationConfig("MetadataServiceURLParamsMetadataCustomizerTest");
        applicationConfig.setMetadataType(DEFAULT_METADATA_STORAGE_TYPE);

        providerBootstrap
            .application(applicationConfig)
            .registry(new RegistryConfig("N/A"))
            .protocol(new ProtocolConfig("dubbo", 2002))
            .service(serviceConfig);

        // will start exporter.export()
        providerBootstrap.start();

        ApplicationModel applicationModel = providerBootstrap.getApplicationModel();
        MetadataServiceURLParamsMetadataCustomizer customizer = new MetadataServiceURLParamsMetadataCustomizer();
        customizer.customize(instance, applicationModel);

        String val = instance.getMetadata().get(METADATA_SERVICE_URL_PARAMS_PROPERTY_NAME);
        Assertions.assertNotNull(val);

        Map<String, String> map = JsonUtils.toJavaObject(val, Map.class);
        Assertions.assertEquals(map.get(PORT_KEY), String.valueOf(metadataServiceURL.getPort()));
        Assertions.assertEquals(map.get(PROTOCOL_KEY), metadataServiceURL.getProtocol());
        Assertions.assertEquals(map.get(VERSION_KEY), metadataServiceURL.getVersion());
        Assertions.assertFalse(map.containsKey(TIMESTAMP_KEY));
        Assertions.assertFalse(map.containsKey(GROUP_KEY));
        Assertions.assertFalse(map.containsKey(APPLICATION_KEY));
    }
}
