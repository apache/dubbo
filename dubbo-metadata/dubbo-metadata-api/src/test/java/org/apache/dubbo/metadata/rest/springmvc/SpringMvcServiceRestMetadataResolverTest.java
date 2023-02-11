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
package org.apache.dubbo.metadata.rest.springmvc;

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.metadata.rest.ClassPathServiceRestMetadataReader;
import org.apache.dubbo.metadata.rest.DefaultRestService;
import org.apache.dubbo.metadata.rest.RestService;
import org.apache.dubbo.metadata.rest.ServiceRestMetadata;
import org.apache.dubbo.metadata.rest.SpringRestService;
import org.apache.dubbo.metadata.rest.StandardRestService;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link SpringMvcServiceRestMetadataResolver} Test
 *
 * @since 2.7.9
 */
class SpringMvcServiceRestMetadataResolverTest {

    private SpringMvcServiceRestMetadataResolver instance = new SpringMvcServiceRestMetadataResolver(ApplicationModel.defaultModel());

    @Test
    void testSupports() {
        // Spring MVC RestService class
        assertTrue(instance.supports(SpringRestService.class));
        // JAX-RS RestService class
        assertFalse(instance.supports(StandardRestService.class));
        // Default RestService class
        assertFalse(instance.supports(DefaultRestService.class));
        // No annotated RestService class
        assertFalse(instance.supports(RestService.class));
        // null
        assertFalse(instance.supports(null));
    }

    @Test
    @Disabled
    void testResolve() {
        // Generated by "dubbo-metadata-processor"
        ClassPathServiceRestMetadataReader reader = new ClassPathServiceRestMetadataReader("META-INF/dubbo/spring-mvc-service-rest-metadata.json");
        List<ServiceRestMetadata> serviceRestMetadataList = reader.read();

        ServiceRestMetadata expectedServiceRestMetadata = serviceRestMetadataList.get(0);
        ServiceRestMetadata serviceRestMetadata = instance.resolve(SpringRestService.class);

        assertTrue(CollectionUtils.equals(expectedServiceRestMetadata.getMeta(), serviceRestMetadata.getMeta()));

        assertEquals(expectedServiceRestMetadata, serviceRestMetadata);

    }
}
