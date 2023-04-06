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

package org.apache.dubbo.rpc.protocol.tri.service;

import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.rpc.model.ApplicationModel;
//import org.apache.dubbo.triple.metadata.MetaRequest;
//import org.apache.dubbo.triple.metadata.MetaResponse;
//import org.apache.dubbo.triple.metadata.MetadataService;
//
//import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class TriMetadataServiceImplTest {

    @Test
    void getMetadataTest() {
        MetadataInfo.ServiceInfo serviceInfo = new MetadataInfo.ServiceInfo();
        serviceInfo.setName("org.apache.dubbo.test.TestService:triple");
        serviceInfo.setPath("org.apache.dubbo.test.TestService");
        serviceInfo.setPort(23456);
        serviceInfo.setProtocol("tri");
        Map<String,String> params = new HashMap<>();
        params.put("test","test");
        serviceInfo.setParams(params);

        Map<String, MetadataInfo.ServiceInfo> infos = new HashMap<>();
        infos.put(serviceInfo.getName(),serviceInfo);
        MetadataInfo metadataInfo = new MetadataInfo("testdemo", "wuhfoaiwfiawhfpa",infos);

        ApplicationModel.defaultModel().setAttribute("metadata",metadataInfo);
//        MetadataService metaService = new TriMetadataServiceImpl();
//        MetaResponse metadata = metaService.getMetadata(MetaRequest.newBuilder().build());
//        Assertions.assertEquals("testdemo",metadata.getApp());
    }
}
