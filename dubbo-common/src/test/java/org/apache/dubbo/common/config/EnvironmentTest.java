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
package org.apache.dubbo.common.config;

import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 */
public class EnvironmentTest {

    @Test
    public void testResolvePlaceholders1() {
        Environment environment = ApplicationModel.defaultModel().getModelEnvironment();

        Map<String, String> externalMap = new LinkedHashMap<>();
        externalMap.put("zookeeper.address", "127.0.0.1");
        externalMap.put("zookeeper.port", "2181");
        environment.updateAppExternalConfigMap(externalMap);

        Map<String, String> sysprops = new LinkedHashMap<>();
        sysprops.put("zookeeper.address", "192.168.10.1");
        System.getProperties().putAll(sysprops);

        try {
            String s = environment.resolvePlaceholders("zookeeper://${zookeeper.address}:${zookeeper.port}");
            assertEquals("zookeeper://192.168.10.1:2181", s);
        } finally {
            for (String key : sysprops.keySet()) {
                System.clearProperty(key);
            }
        }

    }
}
