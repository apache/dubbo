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
package org.apache.dubbo.metadata;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.file.FileSystemDynamicConfiguration;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.service.EchoService;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static java.util.Collections.singleton;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.SUBSCRIBED_SERVICE_NAMES_KEY;
import static org.apache.dubbo.common.utils.CollectionUtils.ofSet;
import static org.apache.dubbo.metadata.DynamicConfigurationServiceNameMapping.buildGroup;
import static org.apache.dubbo.rpc.model.ApplicationModel.getApplicationConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * {@link CompositeServiceNameMapping} Test
 *
 * @since 2.7.8
 */
public class CompositeServiceNameMappingTest {

    private static final URL BASE_URL = URL.valueOf("dubbo://127.0.0.1:20880")
            .setPath(EchoService.class.getName())
            .addParameter(GROUP_KEY, "default")
            .addParameter(VERSION_KEY, "1.0.0");

    private static final String APP_NAME = "test-service";

    private ServiceNameMapping serviceNameMapping;

    private FileSystemDynamicConfiguration dynamicConfiguration;

    @BeforeEach
    public void init() {
        serviceNameMapping = ServiceNameMapping.getDefaultExtension();
        dynamicConfiguration = new FileSystemDynamicConfiguration();
        ApplicationModel.getConfigManager().setApplication(new ApplicationConfig(APP_NAME));
        ApplicationModel.getEnvironment().setDynamicConfiguration(dynamicConfiguration);
    }

    @AfterEach
    public void reset() {
        FileUtils.deleteQuietly(dynamicConfiguration.getRootDirectory());
        ApplicationModel.reset();
    }

    @Test
    public void testType() {
        assertEquals(CompositeServiceNameMapping.class, serviceNameMapping.getClass());
    }

    @Test
    public void testMap() {
        serviceNameMapping.map(BASE_URL);
        assertNotNull(dynamicConfiguration.getConfig(APP_NAME,
                buildGroup(BASE_URL.getServiceInterface(), null, null, null)));
    }

    @Test
    public void testGet() {
        serviceNameMapping.map(BASE_URL);
        Set<String> serviceNames = serviceNameMapping.get(BASE_URL);
        assertEquals(singleton(APP_NAME), serviceNames);

        getApplicationConfig().setName("service1");
        serviceNameMapping.map(BASE_URL);
        serviceNames = serviceNameMapping.get(BASE_URL);
        assertEquals(ofSet(APP_NAME, "service1"), serviceNames);

        serviceNames = serviceNameMapping.get(BASE_URL
                .setPath("com.acme.Interface1")
                .removeParameter(VERSION_KEY)
        );
        assertEquals(singleton("Service1"), serviceNames);

        serviceNames = serviceNameMapping.get(BASE_URL.addParameter(SUBSCRIBED_SERVICE_NAMES_KEY, "s1 , s2 , s3 "));
        assertEquals(ofSet("s1", "s2", "s3"), serviceNames);
    }
}

