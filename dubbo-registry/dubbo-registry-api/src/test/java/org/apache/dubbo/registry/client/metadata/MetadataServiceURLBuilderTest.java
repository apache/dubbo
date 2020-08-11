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

import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;

/**
 * {@link MetadataServiceURLBuilder} Test
 *
 * @since 2.7.5
 */
public class MetadataServiceURLBuilderTest {

    static ServiceInstance serviceInstance = new DefaultServiceInstance("127.0.0.1", "test", 8080);

    static {
        serviceInstance.getMetadata().put("dubbo.metadata-service.urls", "[ \"dubbo://192.168.0.102:20881/com.alibaba.cloud.dubbo.service.DubboMetadataService?anyhost=true&application=spring-cloud-alibaba-dubbo-provider&bind.ip=192.168.0.102&bind.port=20881&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&group=spring-cloud-alibaba-dubbo-provider&interface=com.alibaba.cloud.dubbo.service.DubboMetadataService&methods=getAllServiceKeys,getServiceRestMetadata,getExportedURLs,getAllExportedURLs&pid=17134&qos.enable=false&register=true&release=2.7.3&revision=1.0.0&side=provider&timestamp=1564826098503&version=1.0.0\" ]");
        serviceInstance.getMetadata().put("dubbo.metadata-service.url-params", "{\"dubbo\":{\"application\":\"dubbo-provider-demo\",\"deprecated\":\"false\",\"group\":\"dubbo-provider-demo\",\"version\":\"1.0.0\",\"timestamp\":\"1564845042651\",\"dubbo\":\"2.0.2\",\"provider.host\":\"192.168.0.102\",\"provider.port\":\"20880\"}}");
    }

//    @Test
//    public void testBuild() {
//        assertTrue(composite().build(null).isEmpty());
//        assertTrue(composite().build(new DefaultServiceInstance("127.0.0.1", "test", 8080)).isEmpty());
//        assertEquals(2, composite().build(serviceInstance).size());
//    }
}
