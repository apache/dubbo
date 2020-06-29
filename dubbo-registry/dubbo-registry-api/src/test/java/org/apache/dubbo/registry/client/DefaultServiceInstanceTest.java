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
package org.apache.dubbo.registry.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.lang.String.valueOf;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.METADATA_SERVICE_URLS_PROPERTY_NAME;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.METADATA_SERVICE_URL_PARAMS_PROPERTY_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DefaultServiceInstance} Test
 *
 * @since 2.7.5
 */
public class DefaultServiceInstanceTest {

    public DefaultServiceInstance instance;

    public static DefaultServiceInstance createInstance() {
        DefaultServiceInstance instance = new DefaultServiceInstance(valueOf(System.nanoTime()), "A", "127.0.0.1", 8080);
        instance.getMetadata().put("dubbo.metadata-service.urls", "[ \"dubbo://192.168.0.102:20881/com.alibaba.cloud.dubbo.service.DubboMetadataService?anyhost=true&application=spring-cloud-alibaba-dubbo-provider&bind.ip=192.168.0.102&bind.port=20881&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&group=spring-cloud-alibaba-dubbo-provider&interface=com.alibaba.cloud.dubbo.service.DubboMetadataService&methods=getAllServiceKeys,getServiceRestMetadata,getExportedURLs,getAllExportedURLs&pid=17134&qos.enable=false&register=true&release=2.7.3&revision=1.0.0&side=provider&timestamp=1564826098503&version=1.0.0\" ]");
        instance.getMetadata().put("dubbo.metadata-service.url-params", "{\"dubbo\":{\"application\":\"dubbo-provider-demo\",\"deprecated\":\"false\",\"group\":\"dubbo-provider-demo\",\"version\":\"1.0.0\",\"timestamp\":\"1564845042651\",\"dubbo\":\"2.0.2\",\"provider.host\":\"192.168.0.102\",\"provider.port\":\"20880\"}}");
        return instance;
    }

    @BeforeEach
    public void init() {
        instance = createInstance();
    }

    @Test
    public void testDefaultValues() {
        assertTrue(instance.isEnabled());
        assertTrue(instance.isHealthy());
        assertFalse(instance.getMetadata().isEmpty());
    }

    @Test
    public void testSetAndGetValues() {
        instance.setEnabled(false);
        instance.setHealthy(false);

        assertEquals("A", instance.getServiceName());
        assertEquals("127.0.0.1", instance.getHost());
        assertEquals(8080, instance.getPort());
        assertFalse(instance.isEnabled());
        assertFalse(instance.isHealthy());
        assertFalse(instance.getMetadata().isEmpty());
    }

    @Test
    public void testGetMetadata() {
        assertNotNull(instance.getMetadata(METADATA_SERVICE_URLS_PROPERTY_NAME));
        assertNotNull(instance.getMetadata(METADATA_SERVICE_URL_PARAMS_PROPERTY_NAME));
    }
}
