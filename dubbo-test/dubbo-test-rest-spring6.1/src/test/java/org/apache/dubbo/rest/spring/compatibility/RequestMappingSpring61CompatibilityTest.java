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
package org.apache.dubbo.rest.spring.compatibility;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.nested.RestConfig;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ReflectionServiceDescriptor;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RequestMapping;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.MethodMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ServiceMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.support.spring.SpringMvcRequestMappingResolver;

import java.lang.reflect.Method;
import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.SpringVersion;
import org.springframework.web.bind.annotation.RequestMethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestMappingSpring61CompatibilityTest {

    private FrameworkModel frameworkModel;
    private SpringMvcRequestMappingResolver resolver;

    @BeforeEach
    void setUp() {
        assertTrue(SpringVersion.getVersion().startsWith("6.1."));
        frameworkModel = new FrameworkModel();
        resolver = new SpringMvcRequestMappingResolver(frameworkModel);
        resolver.setRestConfig(new RestConfig());
    }

    @AfterEach
    void tearDown() {
        frameworkModel.destroy();
    }

    @Test
    void resolvesRequestMappingWithSpring61() throws Exception {
        Class<RequestMappingService> serviceType = RequestMappingService.class;
        Method method = serviceType.getMethod("getItem");
        ReflectionServiceDescriptor serviceDescriptor = new ReflectionServiceDescriptor(serviceType);
        MethodDescriptor methodDescriptor = serviceDescriptor.getMethod("getItem", method.getParameterTypes());
        URL url = URL.valueOf("tri://localhost/" + serviceType.getName());
        ServiceMeta serviceMeta = new ServiceMeta(
                Collections.singletonList(serviceType), serviceDescriptor, null, url, resolver.getRestToolKit());
        MethodMeta methodMeta = new MethodMeta(Collections.singletonList(method), methodDescriptor, serviceMeta);

        RequestMapping mapping = resolver.resolve(methodMeta);

        assertEquals(Collections.singleton("GET"), mapping.getMethodsCondition().getMethods());
        assertEquals(
                "/items/{id}",
                mapping.getPathCondition().getExpressions().get(0).getPath());
        assertEquals(
                "application/json",
                mapping.getProducesCondition().getMediaTypes().get(0).getName());
    }

    interface RequestMappingService {

        @org.springframework.web.bind.annotation.RequestMapping(
                path = "/items/{id}",
                method = RequestMethod.GET,
                produces = "application/json")
        String getItem();
    }
}
