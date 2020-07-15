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

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;

import static java.util.Collections.singleton;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SERVICE_NAME_MAPPING_PROPERTIES_FILE_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link PropertiesFileServiceNameMapping} Test
 *
 * @since 2.7.8
 */
public class PropertiesFileServiceNameMappingTest {

    private static final URL BASE_URL = URL.valueOf("dubbo://127.0.0.1:20880");


    @Test
    public void testMap() {
        PropertiesFileServiceNameMapping serviceNameMapping = new PropertiesFileServiceNameMapping();
        serviceNameMapping.map(BASE_URL);
    }

    @Test
    public void testGet() {

        PropertiesFileServiceNameMapping serviceNameMapping = new PropertiesFileServiceNameMapping();
        URL url = BASE_URL.setServiceInterface("com.acme.Interface1").addParameter(GROUP_KEY, "default");
        assertEquals(singleton("Service1"), serviceNameMapping.get(url));

        System.setProperty(SERVICE_NAME_MAPPING_PROPERTIES_FILE_KEY, "///META-INF//dubbo/service-name-mapping.properties");
        serviceNameMapping = new PropertiesFileServiceNameMapping();

        url = BASE_URL.setProtocol("thirft").setServiceInterface("com.acme.InterfaceX");
        assertEquals(new LinkedHashSet<>(Arrays.asList("Service1", "Service2")), serviceNameMapping.get(url));
    }
}
