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
 *//*

package org.apache.dubbo.metadata;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.file.FileSystemDynamicConfiguration;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.SUBSCRIBED_SERVICE_NAMES_KEY;
import static org.apache.dubbo.metadata.DynamicConfigurationServiceNameMapping.buildGroup;
import static org.apache.dubbo.metadata.ServiceNameMapping.getDefaultExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

*/
/**
 * {@link ServiceNameMapping} Test
 *
 * @since 2.7.8
 *//*

public class ServiceNameMappingTest {

    private static final URL BASE_URL = URL.valueOf("dubbo://127.0.0.1:20880");

    private FileSystemDynamicConfiguration configuration;

    private ServiceNameMapping serviceNameMapping;

    private String applicationName;

    @BeforeEach
    public void init() {

        ApplicationModel.reset();

        applicationName = getClass().getSimpleName();

        ApplicationModel.getConfigManager().setApplication(new ApplicationConfig(applicationName));

        configuration = new FileSystemDynamicConfiguration();

        FileUtils.deleteQuietly(configuration.getRootDirectory());

        ApplicationModel.getEnvironment().setDynamicConfiguration(configuration);

        serviceNameMapping = getDefaultExtension();
    }

    @AfterEach
    public void reset() throws Exception {
        FileUtils.deleteQuietly(configuration.getRootDirectory());
        configuration.close();
        ApplicationModel.reset();
    }

    @Test
    public void testDeprecatedMethods() {
        assertThrows(UnsupportedOperationException.class, () -> {
            serviceNameMapping.map(null, null, null, null);
        });

        assertThrows(UnsupportedOperationException.class, () -> {
            serviceNameMapping.get(null, null, null, null);
        });
    }

    @Test
    public void testMap() {
        String serviceInterface = ServiceNameMapping.class.getName();
        String key = applicationName;
        String group = buildGroup(serviceInterface, null, null, null);
        URL url = BASE_URL.setServiceInterface(serviceInterface);
        serviceNameMapping.map(url);
        assertNotNull(configuration.getConfig(key, group));
    }

    @Test
    public void testGet() {
        String serviceInterface = ServiceNameMapping.class.getName();
        URL url = BASE_URL.setServiceInterface(serviceInterface);
        serviceNameMapping.map(url);
        Set<String> serviceNames = serviceNameMapping.get(url);
        assertEquals(singleton(applicationName), serviceNames);

        url = url.setServiceInterface("com.acme.Interface1").addParameter(GROUP_KEY, "default");
        serviceNames = serviceNameMapping.get(url);
        assertEquals(singleton("Service1"), serviceNames);


        url = url.addParameter(SUBSCRIBED_SERVICE_NAMES_KEY, "A , B , C  ");
        serviceNames = serviceNameMapping.get(url);
        assertEquals(new LinkedHashSet<>(asList("A", "B", "C")), serviceNames);

    }
}
*/
