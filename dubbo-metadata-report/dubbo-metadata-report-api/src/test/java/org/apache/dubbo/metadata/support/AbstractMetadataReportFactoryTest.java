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
package org.apache.dubbo.metadata.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.metadata.definition.model.FullServiceDefinition;
import org.apache.dubbo.metadata.identifier.MetadataIdentifier;
import org.apache.dubbo.metadata.store.MetadataReport;

import com.alibaba.fastjson.JSON;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 2018/9/14
 */
public class AbstractMetadataReportFactoryTest {

    private AbstractMetadataReportFactory metadataReportFactory = new AbstractMetadataReportFactory() {
        @Override
        protected MetadataReport createMetadataReport(URL url) {
            return new MetadataReport() {

                @Override
                public void storeProviderMetadata(MetadataIdentifier providerMetadataIdentifier, FullServiceDefinition serviceDefinition) {
                    store.put(providerMetadataIdentifier.getIdentifierKey(), JSON.toJSONString(serviceDefinition));
                }

                @Override
                public void storeConsumerMetadata(MetadataIdentifier consumerMetadataIdentifier, Map serviceParameterMap) {
                    store.put(consumerMetadataIdentifier.getIdentifierKey(), JSON.toJSONString(serviceParameterMap));
                }

                Map<String, String> store = new ConcurrentHashMap<>();


            };
        }
    };

    @Test
    public void testGetOneMetadataReport() {
        URL url = URL.valueOf("zookeeper://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic");
        MetadataReport metadataReport1 = metadataReportFactory.getMetadataReport(url);
        MetadataReport metadataReport2 = metadataReportFactory.getMetadataReport(url);
        Assert.assertEquals(metadataReport1, metadataReport2);
    }

    @Test
    public void testGetOneMetadataReportForIpFormat() {
        URL url1 = URL.valueOf("zookeeper://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic");
        URL url2 = URL.valueOf("zookeeper://" + NetUtils.getLocalAddress().getHostAddress() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic");
        MetadataReport metadataReport1 = metadataReportFactory.getMetadataReport(url1);
        MetadataReport metadataReport2 = metadataReportFactory.getMetadataReport(url2);
        Assert.assertEquals(metadataReport1, metadataReport2);
    }

    @Test
    public void testGetForDiffService() {
        URL url1 = URL.valueOf("zookeeper://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService1?version=1.0.0&application=vic");
        URL url2 = URL.valueOf("zookeeper://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService2?version=1.0.0&application=vic");
        MetadataReport metadataReport1 = metadataReportFactory.getMetadataReport(url1);
        MetadataReport metadataReport2 = metadataReportFactory.getMetadataReport(url2);
        Assert.assertEquals(metadataReport1, metadataReport2);
    }

    @Test
    public void testGetForDiffGroup() {
        URL url1 = URL.valueOf("zookeeper://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic&group=aaa");
        URL url2 = URL.valueOf("zookeeper://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic&group=bbb");
        MetadataReport metadataReport1 = metadataReportFactory.getMetadataReport(url1);
        MetadataReport metadataReport2 = metadataReportFactory.getMetadataReport(url2);
        Assert.assertNotEquals(metadataReport1, metadataReport2);
    }
}
