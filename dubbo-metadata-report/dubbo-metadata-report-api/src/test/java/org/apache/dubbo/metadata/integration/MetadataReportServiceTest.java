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
package org.apache.dubbo.metadata.integration;

import com.google.gson.Gson;
import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.metadata.definition.model.FullServiceDefinition;
import org.apache.dubbo.metadata.store.test.JTestMetadataReport4Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 2018/9/14
 */
public class MetadataReportServiceTest {
    URL url = URL.valueOf("JTest://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic");
    MetadataReportService metadataReportService1;

    @BeforeEach
    public void before() {

        metadataReportService1 = MetadataReportService.instance(new Supplier<URL>() {
            @Override
            public URL get() {
                return url;
            }
        });
    }

    @Test
    public void testInstance() {

        MetadataReportService metadataReportService2 = MetadataReportService.instance(new Supplier<URL>() {
            @Override
            public URL get() {
                return url;
            }
        });
        Assertions.assertSame(metadataReportService1, metadataReportService2);
        Assertions.assertEquals(metadataReportService1.metadataReportUrl, url);
    }

    @Test
    public void testPublishProviderNoInterfaceName() {


        URL publishUrl = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vicpubprovder&side=provider");
        metadataReportService1.publishProvider(publishUrl);

        Assertions.assertTrue(metadataReportService1.metadataReport instanceof JTestMetadataReport4Test);

        JTestMetadataReport4Test jTestMetadataReport4Test = (JTestMetadataReport4Test) metadataReportService1.metadataReport;
        Assertions.assertTrue(!jTestMetadataReport4Test.store.containsKey(JTestMetadataReport4Test.getProviderKey(publishUrl)));

    }

    @Test
    public void testPublishProviderWrongInterface() {

        URL publishUrl = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vicpu&interface=ccc&side=provider");
        metadataReportService1.publishProvider(publishUrl);

        Assertions.assertTrue(metadataReportService1.metadataReport instanceof JTestMetadataReport4Test);

        JTestMetadataReport4Test jTestMetadataReport4Test = (JTestMetadataReport4Test) metadataReportService1.metadataReport;
        Assertions.assertTrue(!jTestMetadataReport4Test.store.containsKey(JTestMetadataReport4Test.getProviderKey(publishUrl)));

    }

    @Test
    public void testPublishProviderContainInterface() throws InterruptedException {

        URL publishUrl = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.3&application=vicpubp&interface=org.apache.dubbo.metadata.integration.InterfaceNameTestService&side=provider");
        metadataReportService1.publishProvider(publishUrl);
        Thread.sleep(300);

        Assertions.assertTrue(metadataReportService1.metadataReport instanceof JTestMetadataReport4Test);

        JTestMetadataReport4Test jTestMetadataReport4Test = (JTestMetadataReport4Test) metadataReportService1.metadataReport;
        Assertions.assertTrue(jTestMetadataReport4Test.store.containsKey(JTestMetadataReport4Test.getProviderKey(publishUrl)));

        String value = jTestMetadataReport4Test.store.get(JTestMetadataReport4Test.getProviderKey(publishUrl));
        FullServiceDefinition fullServiceDefinition = toServiceDefinition(value);
        Map<String,String> map = fullServiceDefinition.getParameters();
        Assertions.assertEquals(map.get("application"), "vicpubp");
        Assertions.assertEquals(map.get("version"), "1.0.3");
        Assertions.assertEquals(map.get("interface"), "org.apache.dubbo.metadata.integration.InterfaceNameTestService");
    }

    @Test
    public void testPublishConsumer() throws InterruptedException {

        URL publishUrl = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.x&application=vicpubconsumer&side=consumer");
        metadataReportService1.publishConsumer(publishUrl);
        Thread.sleep(300);

        Assertions.assertTrue(metadataReportService1.metadataReport instanceof JTestMetadataReport4Test);

        JTestMetadataReport4Test jTestMetadataReport4Test = (JTestMetadataReport4Test) metadataReportService1.metadataReport;
        Assertions.assertTrue(jTestMetadataReport4Test.store.containsKey(JTestMetadataReport4Test.getConsumerKey(publishUrl)));

        String value = jTestMetadataReport4Test.store.get(JTestMetadataReport4Test.getConsumerKey(publishUrl));
        Gson gson = new Gson();
        Map<String, String> map = gson.fromJson(value, Map.class);
        Assertions.assertEquals(map.get("application"), "vicpubconsumer");
        Assertions.assertEquals(map.get("version"), "1.0.x");

    }

    private FullServiceDefinition toServiceDefinition(String urlQuery) {
        Gson gson = new Gson();
        return gson.fromJson(urlQuery, FullServiceDefinition.class);
    }

}
