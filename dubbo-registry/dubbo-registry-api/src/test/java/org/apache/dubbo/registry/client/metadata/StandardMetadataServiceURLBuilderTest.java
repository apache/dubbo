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
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.apache.dubbo.registry.client.metadata.MetadataServiceURLBuilderTest.serviceInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;

/**
 * {@link StandardMetadataServiceURLBuilder} Test
 */
public class StandardMetadataServiceURLBuilderTest {

    private StandardMetadataServiceURLBuilder builder = new StandardMetadataServiceURLBuilder();

    @Test
    public void testBuild() {
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setMetadataServicePort(7001);
        ApplicationModel applicationModel = spy(ApplicationModel.defaultModel());
        applicationModel.getApplicationConfigManager().setApplication(applicationConfig);

        List<URL> urls = builder.build(new DefaultServiceInstance("test", "127.0.0.1", 8080, ApplicationModel.defaultModel()));
        assertEquals(1, urls.size());
        assertEquals(urls.get(0).toString(), "dubbo://127.0.0.1:7001/org.apache.dubbo.metadata.MetadataService?getAndListenInstanceMetadata.1.callback=true&group=test&reconnect=false&side=consumer&timeout=5000&version=1.0.0");

        urls = builder.build(serviceInstance);
        assertEquals(1, urls.size());
        assertEquals(urls.get(0).toString(), "dubbo://127.0.0.1:20880/org.apache.dubbo.metadata.MetadataService?application=dubbo-provider-demo&dubbo=2.0.2&group=test&host=192.168.0.102&port=20880&protocol=dubbo&side=consumer&timeout=5000&timestamp=1564845042651&version=1.0.0");
    }

}
