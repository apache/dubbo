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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link ServiceInstanceMetadataUtils} Test
 *
 * @since 2.7.5
 */
public class ServiceInstanceMetadataUtilsTest {

    private static URL url = URL.valueOf("dubbo://192.168.0.102:20880/org.apache.dubbo.metadata.MetadataService?&anyhost=true&application=spring-cloud-alibaba-dubbo-provider&bind.ip=192.168.0.102&bind.port=20880&default.deprecated=false&default.dynamic=false&default.register=true&deprecated=false&dubbo=2.0.2&dynamic=false&generic=false&group=spring-cloud-alibaba-dubbo-provider&interface=org.apache.dubbo.metadata.MetadataService&methods=getAllServiceKeys,getServiceRestMetadata,getExportedURLs,getAllExportedURLs&pid=58350&register=true&release=2.7.1&revision=1.0.0&side=provider&timestamp=1557928573174&version=1.0.0");
    private static URL url2 = URL.valueOf("rest://192.168.0.102:20880/org.apache.dubbo.metadata.MetadataService?&anyhost=true&application=spring-cloud-alibaba-dubbo-provider&bind.ip=192.168.0.102&bind.port=20880&default.deprecated=false&default.dynamic=false&default.register=true&deprecated=false&dubbo=2.0.2&dynamic=false&generic=false&group=spring-cloud-alibaba-dubbo-provider&interface=org.apache.dubbo.metadata.MetadataService&methods=getAllServiceKeys,getServiceRestMetadata,getExportedURLs,getAllExportedURLs&pid=58350&register=true&release=2.7.1&revision=1.0.0&side=provider&timestamp=1557928573174&version=1.0.0");

    private static final String VALUE = "{\"rest\":{\"version\":\"1.0.0\",\"dubbo\":\"2.0.2\",\"release\":\"2.7.1\",\"port\":\"20880\"},\"dubbo\":{\"version\":\"1.0.0\",\"dubbo\":\"2.0.2\",\"release\":\"2.7.1\",\"port\":\"20880\"}}";
    @Test
    public void testMetadataServiceURLParameters() {

        List<URL> urls = Arrays.asList(url, url2);

        String parameter = ServiceInstanceMetadataUtils.getMetadataServiceParameter(urls);

        JSONObject jsonObject = JSON.parseObject(parameter);

        urls.forEach(url -> {
            JSONObject map = jsonObject.getJSONObject(url.getProtocol());
            for (Map.Entry<String, String> param : url.getParameters().entrySet()) {
                String value = map.getString(param.getKey());
                if (value != null) {
                    assertEquals(param.getValue(), value);
                }
            }
        });

        assertEquals(VALUE, parameter);
    }

    @Test
    public void testProtocolPorts() {

//        Map<String, String> metadata = new LinkedHashMap<>();
//
//        String key = protocolPortMetadataKey("dubbo");
//        assertEquals("dubbo.protocols.dubbo.port", key);
//
//        metadata.put(key, "20880");
//
//        key = protocolPortMetadataKey("rest");
//        assertEquals("dubbo.protocols.rest.port", key);
//
//        metadata.put(key, "8080");
//
//        Map<String, Integer> protocolPorts = getProtocolPorts(metadata);
//
//        Map<String, Integer> expected = new LinkedHashMap<>();
//
//        expected.put("dubbo", 20880);
//        expected.put("rest", 8080);
//
//        assertEquals(expected, protocolPorts);
    }
}
