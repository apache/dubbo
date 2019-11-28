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
//package org.apache.dubbo.registry.client.fastjson;
//
//import org.apache.dubbo.registry.client.DefaultServiceInstance;
//
//import com.alibaba.fastjson.JSON;
//import com.alibaba.fastjson.parser.ParserConfig;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.Test;
//
///**
// * {@link DefaultServiceInstanceDeserializer} Test
// *
// * @since 2.7.5
// */
//public class DefaultServiceInstanceDeserializerTest {
//
//    private static final String JSON_CONTENT = "{\n" +
//            "  \"enabled\": true,\n" +
//            "  \"healthy\": true,\n" +
//            "  \"host\": \"fe80:0:0:0:1c49:6eff:fe54:2495%7\",\n" +
//            "  \"metadata\": {\n" +
//            "    \"dubbo.metadata-service.url-params\": \"{\\\"dubbo\\\":{\\\"application\\\":\\\"dubbo-provider-demo\\\",\\\"deprecated\\\":\\\"false\\\",\\\"group\\\":\\\"dubbo-provider-demo\\\",\\\"version\\\":\\\"1.0.0\\\",\\\"timestamp\\\":\\\"1566132738256\\\",\\\"dubbo\\\":\\\"2.0.2\\\",\\\"provider.host\\\":\\\"fe80:0:0:0:1c49:6eff:fe54:2495%7\\\",\\\"provider.port\\\":\\\"20880\\\"}}\",\n" +
//            "    \"dubbo.subscribed-services.revision\": \"1\",\n" +
//            "    \"dubbo.metadata.storage-type\": \"default\",\n" +
//            "    \"dubbo.exported-services.revision\": \"640372560\"\n" +
//            "  },\n" +
//            "  \"port\": 20880,\n" +
//            "  \"serviceName\": \"dubbo-provider-demo\"\n" +
//            "}";
//
//    @BeforeAll
//    public static void init() {
//        ParserConfig.getGlobalInstance().putDeserializer(DefaultServiceInstance.class, new DefaultServiceInstanceDeserializer());
//    }
//
//    @Test
//    public void testDeserialze() {
//
//        DefaultServiceInstance serviceInstance = JSON.parseObject(JSON_CONTENT, DefaultServiceInstance.class);
//
//    }
//}
