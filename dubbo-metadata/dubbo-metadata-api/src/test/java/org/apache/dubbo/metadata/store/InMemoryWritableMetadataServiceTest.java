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
package org.apache.dubbo.metadata.store;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 2019-08-29
 */
public class InMemoryWritableMetadataServiceTest {

    String interfaceName = "org.apache.dubbo.metadata.store.InterfaceNameTestService2", version = "0.9.9", group = null;
    URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/?interface=" + interfaceName + "&version="
            + version + "&application=vicpubprovder&side=provider");

    @BeforeEach
    public void before() {
    }

    @Test
    public void testPublishServiceDefinition() {
        InMemoryWritableMetadataService inMemoryWritableMetadataService = new InMemoryWritableMetadataService();
        inMemoryWritableMetadataService.publishServiceDefinition(url);

        String v = inMemoryWritableMetadataService.getServiceDefinition(interfaceName, version, group);
        Assertions.assertNotNull(v);
    }

    @Test
    public void testExportURL() {
        InMemoryWritableMetadataService inMemoryWritableMetadataService = new InMemoryWritableMetadataService();
        URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.Test567Service?version=1.0.44&application=vicpubprovder&side=provider");
        inMemoryWritableMetadataService.exportURL(url);

        Assertions.assertTrue(inMemoryWritableMetadataService.getExportedServiceURLs().size() == 1);
        Assertions.assertEquals(inMemoryWritableMetadataService.getExportedServiceURLs().get(url.getServiceKey()).first(), url);
    }

    @Test
    public void testSubscribeURL() {
        InMemoryWritableMetadataService inMemoryWritableMetadataService = new InMemoryWritableMetadataService();
        URL url = URL.valueOf("subscriber://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.Test678Service?version=1.0.44&application=vicpubprovder&side=provider");
        inMemoryWritableMetadataService.subscribeURL(url);

        Assertions.assertTrue(inMemoryWritableMetadataService.getSubscribedServiceURLs().size() == 1);
        Assertions.assertEquals(inMemoryWritableMetadataService.getSubscribedServiceURLs().get(url.getServiceKey()).first(), url);
    }

    @Test
    public void testUnExportURL() {
        InMemoryWritableMetadataService inMemoryWritableMetadataService = new InMemoryWritableMetadataService();
        URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.Test567Service?version=1.0.44&application=vicpubprovder&side=provider");
        inMemoryWritableMetadataService.exportURL(url);

        Assertions.assertTrue(inMemoryWritableMetadataService.getExportedServiceURLs().size() == 1);
        Assertions.assertEquals(inMemoryWritableMetadataService.getExportedServiceURLs().get(url.getServiceKey()).first(), url);

        inMemoryWritableMetadataService.unexportURL(url);
        Assertions.assertTrue(inMemoryWritableMetadataService.getExportedServiceURLs().size() == 0);
    }

    @Test
    public void testUnSubscribeURL() {
        InMemoryWritableMetadataService inMemoryWritableMetadataService = new InMemoryWritableMetadataService();
        URL url = URL.valueOf("subscriber://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.Test678Service?version=1.0.44&application=vicpubprovder&side=provider");
        inMemoryWritableMetadataService.subscribeURL(url);

        Assertions.assertTrue(inMemoryWritableMetadataService.getSubscribedServiceURLs().size() == 1);
        Assertions.assertEquals(inMemoryWritableMetadataService.getSubscribedServiceURLs().get(url.getServiceKey()).first(), url);

        inMemoryWritableMetadataService.unsubscribeURL(url);
        Assertions.assertTrue(inMemoryWritableMetadataService.getSubscribedServiceURLs().size() == 0);
    }

}
