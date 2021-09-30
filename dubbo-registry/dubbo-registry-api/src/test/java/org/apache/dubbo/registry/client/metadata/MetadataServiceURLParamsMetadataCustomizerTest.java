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
package org.apache.dubbo.registry.client.metadata;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.metadata.store.InMemoryWritableMetadataService;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PORT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMESTAMP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.METADATA_SERVICE_URL_PARAMS_PROPERTY_NAME;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MetadataServiceURLParamsMetadataCustomizerTest {
    private static final Gson gson = new Gson();

    public DefaultServiceInstance instance;
    private InMemoryWritableMetadataService metadataService;
    private URL metadataServiceURL = URL.valueOf("metadata://127.0.0.1:21881/" + MetadataService.class.getName() +
        "?application=demo&group=g1&version=1.0.0&timestamp=1632662388960");


    public static DefaultServiceInstance createInstance() {
        return new DefaultServiceInstance("A", "127.0.0.1", 20880, ApplicationModel.defaultModel());
    }

    @BeforeEach
    public void init() {
        instance = createInstance();
        metadataService = mock(InMemoryWritableMetadataService.class);
        when(metadataService.getMetadataServiceURL()).thenReturn(metadataServiceURL);
    }

    @AfterEach
    public void tearDown() throws IOException {
        Mockito.framework().clearInlineMocks();
    }

    @Test
    public void test() {
        MetadataServiceURLParamsMetadataCustomizer customizer = new MetadataServiceURLParamsMetadataCustomizer();
        try (MockedStatic<WritableMetadataService> mockMetadataService = Mockito.mockStatic(WritableMetadataService.class)) {
            mockMetadataService.when(() -> WritableMetadataService.getDefaultExtension(ApplicationModel.defaultModel())).thenReturn(metadataService);
            customizer.customize(instance);

            String val = instance.getMetadata().get(METADATA_SERVICE_URL_PARAMS_PROPERTY_NAME);
            Assertions.assertNotNull(val);

            Map<String, String> map = gson.fromJson(val, new TypeToken<Map<String, String>>() {
            }.getType());
            Assertions.assertEquals(map.get(PORT_KEY), String.valueOf(metadataServiceURL.getPort()));
            Assertions.assertEquals(map.get(PROTOCOL_KEY), metadataServiceURL.getProtocol());
            Assertions.assertEquals(map.get(VERSION_KEY), metadataServiceURL.getVersion());
            Assertions.assertFalse(map.containsKey(TIMESTAMP_KEY));
            Assertions.assertFalse(map.containsKey(GROUP_KEY));
            Assertions.assertFalse(map.containsKey(APPLICATION_KEY));
        }
    }
}
