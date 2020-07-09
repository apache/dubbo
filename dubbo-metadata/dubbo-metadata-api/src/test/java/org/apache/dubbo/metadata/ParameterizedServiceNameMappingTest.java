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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.apache.dubbo.common.constants.RegistryConstants.SUBSCRIBED_SERVICE_NAMES_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link ParameterizedServiceNameMapping} Test
 *
 * @see ParameterizedServiceNameMapping
 * @since 2.7.8
 */
public class ParameterizedServiceNameMappingTest {

    private static final URL BASE_URL = URL.valueOf("dubbo://127.0.0.1:20880");

    private ParameterizedServiceNameMapping serviceNameMapping;

    @BeforeEach
    public void init() {
        serviceNameMapping = new ParameterizedServiceNameMapping();
    }

    @Test
    public void testMap() {
        // NOTHING to happen
        serviceNameMapping.map(BASE_URL);
    }

    @Test
    public void testGet() {
        Set<String> serviceNames = serviceNameMapping.get(BASE_URL);
        assertEquals(emptySet(), serviceNames);

        serviceNames = serviceNameMapping.get(BASE_URL.addParameter(SUBSCRIBED_SERVICE_NAMES_KEY, "    Service1     "));
        assertEquals(singleton("Service1"), serviceNames);

        serviceNames = serviceNameMapping.get(BASE_URL.addParameter(SUBSCRIBED_SERVICE_NAMES_KEY, "Service1 ,  Service2   "));
        assertEquals(new LinkedHashSet(Arrays.asList("Service1", "Service2")), serviceNames);
    }
}
