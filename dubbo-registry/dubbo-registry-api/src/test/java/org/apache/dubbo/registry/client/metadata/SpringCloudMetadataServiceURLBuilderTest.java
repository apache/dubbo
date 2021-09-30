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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.apache.dubbo.registry.client.metadata.MetadataServiceURLBuilderTest.serviceInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link SpringCloudMetadataServiceURLBuilder} Test
 *
 * @since 2.7.5
 */
public class SpringCloudMetadataServiceURLBuilderTest {

    private SpringCloudMetadataServiceURLBuilder builder = new SpringCloudMetadataServiceURLBuilder();

    @Test
    public void testBuild() {
        List<URL> urls = builder.build(new DefaultServiceInstance("127.0.0.1", "test", 8080, ApplicationModel.defaultModel()));
        assertEquals(0, urls.size());

        urls = builder.build(serviceInstance);
        assertEquals(1, urls.size());
        URL url = urls.get(0);
        assertEquals("192.168.0.102", url.getHost());
        assertEquals(20881, url.getPort());
        assertEquals("com.alibaba.cloud.dubbo.service.DubboMetadataService", url.getServiceInterface());
    }

}
