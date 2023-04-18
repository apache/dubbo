///*
// * Licensed to the Apache Software Foundation (ASF) under one or more
// * contributor license agreements.  See the NOTICE file distributed with
// * this work for additional information regarding copyright ownership.
// * The ASF licenses this file to You under the Apache License, Version 2.0
// * (the "License"); you may not use this file except in compliance with
// * the License.  You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package org.apache.dubbo.rpc.protocol.tri.service;
//
//import org.apache.dubbo.common.URL;
//import org.apache.dubbo.metadata.InstanceMetadataChangedListener;
//import org.apache.dubbo.metadata.MetadataInfo;
//import org.apache.dubbo.metadata.MetadataService;
//import org.apache.dubbo.metadata.TriMetadataServiceImpl;
//import org.apache.dubbo.rpc.model.ApplicationModel;
//
//import org.apache.dubbo.triple.metadata.AllMetaRequest;
//import org.apache.dubbo.triple.metadata.AllMetaResponse;
//import org.apache.dubbo.triple.metadata.Metadata;
//import org.apache.dubbo.triple.metadata.ResponseStatus;
//import org.apache.dubbo.triple.metadata.MetaRequest;
//import org.apache.dubbo.triple.metadata.MetaResponse;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.Test;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.SortedSet;
//
//
//class TriMetadataServiceImplTest {
//
//    @BeforeAll
//    static void registerMetadataService() {
//        Metadata triMetadataService = new TriMetadataServiceImpl();
//        AllMetaResponse allMetadata = triMetadataService.getAllMetadata(AllMetaRequest.newBuilder().build());
//        Assertions.assertEquals(ResponseStatus.SERVICE_NOT_REGISTER, allMetadata.getStatus());
//        ApplicationModel.defaultModel().getBeanFactory().registerBean(new TestMetadataServiceImpl());
//
//    }
//
//    @Test
//    void getMetadataTest() {
//        Metadata triMetadataService = new TriMetadataServiceImpl();
//        MetaResponse test1 = triMetadataService.getMetadata(MetaRequest.newBuilder().setRevision("test").build());
//        Assertions.assertEquals(ResponseStatus.SUCCESS, test1.getStatus());
//        Assertions.assertEquals("testdemo", test1.getApp());
//
//        MetaResponse test2 = triMetadataService.getMetadata(MetaRequest.newBuilder().build());
//        Assertions.assertEquals(ResponseStatus.REVISION_UN_FIND, test2.getStatus());
//    }
//
//    @Test
//    void getAllMetadataTest() {
//        Metadata triMetadataService = new TriMetadataServiceImpl();
//
//        AllMetaResponse test1 = triMetadataService.getAllMetadata(AllMetaRequest.newBuilder().build());
//        Assertions.assertEquals(ResponseStatus.SUCCESS, test1.getStatus());
//        Assertions.assertEquals("testdemo", test1.getAllMetadata(0).getApp());
//    }
//
//    static class TestMetadataServiceImpl implements MetadataService {
//        @Override
//        public String serviceName() {
//            return null;
//        }
//
//        @Override
//        public URL getMetadataURL() {
//            return null;
//        }
//
//        @Override
//        public SortedSet<String> getExportedURLs(String serviceInterface, String group, String version, String protocol) {
//            return null;
//        }
//
//        @Override
//        public String getServiceDefinition(String serviceKey) {
//            return null;
//        }
//
//        @Override
//        public MetadataInfo getMetadataInfo(String revision) {
//            if ("test".equals(revision)) {
//                return getMetadataInfo();
//            }
//            return null;
//        }
//
//        private MetadataInfo getMetadataInfo() {
//            MetadataInfo.ServiceInfo serviceInfo = new MetadataInfo.ServiceInfo();
//            serviceInfo.setName("org.apache.dubbo.test.TestService:triple");
//            serviceInfo.setPath("org.apache.dubbo.test.TestService");
//            serviceInfo.setPort(23456);
//            serviceInfo.setProtocol("tri");
//            Map<String, String> params = new HashMap<>();
//            params.put("test", "test");
//            serviceInfo.setParams(params);
//            Map<String, MetadataInfo.ServiceInfo> infos = new HashMap<>();
//            infos.put(serviceInfo.getName(), serviceInfo);
//            return new MetadataInfo("testdemo", "test", infos);
//        }
//
//        @Override
//        public List<MetadataInfo> getMetadataInfos() {
//            List<MetadataInfo> metadataInfos = new ArrayList<>();
//            metadataInfos.add(getMetadataInfo());
//            return metadataInfos;
//        }
//
//        @Override
//        public void exportInstanceMetadata(String instanceMetadata) {
//
//        }
//
//        @Override
//        public Map<String, InstanceMetadataChangedListener> getInstanceMetadataChangedListenerMap() {
//            return null;
//        }
//
//        @Override
//        public String getAndListenInstanceMetadata(String consumerId, InstanceMetadataChangedListener listener) {
//            return null;
//        }
//    }
//}
