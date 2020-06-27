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

package org.apache.dubbo.config;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ConfigCenterConfigTest {
    @Test
    public void testPrefix() {
        ConfigCenterConfig config = new ConfigCenterConfig();
        Assertions.assertEquals("dubbo.config-center", config.getPrefix());
    }

    @Test
    public void testToUrl() {
        ConfigCenterConfig config = new ConfigCenterConfig();
        config.setNamespace("namespace");
        config.setGroup("group");
        config.setAddress("zookeeper://127.0.0.1:2181");

        Assertions.assertEquals("zookeeper://127.0.0.1:2181/ConfigCenterConfig?check=true&" +
                        "config-file=dubbo.properties&group=group&highest-priority=true&" +
                        "namespace=namespace&timeout=3000",
                config.toUrl().toFullString()
        );
    }
}
