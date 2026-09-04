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
package org.apache.dubbo.rpc.protocol.tri.rest.support.spring;

import org.apache.dubbo.common.extension.Activate;

import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.SpringVersion;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestSpringBoot3ClasspathTest {

    private static final String ADAPTER_RESOURCE =
            "org/apache/dubbo/rpc/protocol/tri/rest/support/spring/HandlerInterceptorAdapter.class";
    private static final String COMMON_AUTO_CONFIGURATION_RESOURCE =
            "org/apache/dubbo/spring/boot/autoconfigure/DubboAutoConfiguration.class";
    private static final String BOOT3_AUTO_CONFIGURATION_RESOURCE =
            "org/apache/dubbo/spring/boot/autoconfigure/DubboTriple3AutoConfiguration.class";
    private static final String LEGACY_MARKER = "META-INF/dubbo/rest-spring-javax.properties";
    private static final String SPRING6_MARKER = "META-INF/dubbo/rest-spring-jakarta.properties";

    @Test
    void starterBoot3AutoconfigureAndExplicitAdapterSelectOnlySpring6JakartaImplementation() throws Exception {
        ClassLoader classLoader = RestSpringBoot3ClasspathTest.class.getClassLoader();
        assertNotNull(classLoader.getResource(COMMON_AUTO_CONFIGURATION_RESOURCE));
        assertNotNull(classLoader.getResource(BOOT3_AUTO_CONFIGURATION_RESOURCE));

        List<URL> adapters = Collections.list(classLoader.getResources(ADAPTER_RESOURCE));
        assertEquals(1, adapters.size(), () -> "Expected one REST Spring adapter but found " + adapters);
        assertTrue(SpringVersion.getVersion().startsWith("6."));
        assertNull(classLoader.getResource(LEGACY_MARKER));
        assertNotNull(classLoader.getResource(SPRING6_MARKER));

        Activate activation = HandlerInterceptorAdapter.class.getAnnotation(Activate.class);
        assertNotNull(activation);
        assertArrayEquals(
                new String[] {
                    "org.springframework.web.servlet.HandlerInterceptor", "jakarta.servlet.http.HttpServletRequest"
                },
                activation.onClass());
    }
}
