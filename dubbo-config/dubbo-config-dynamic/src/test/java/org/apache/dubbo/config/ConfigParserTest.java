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

import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;

/**
 *
 */
public class ConfigParserTest {
    @Test
    public void parseConfiguratorsServiceNoAppTest() throws Exception {
        InputStream ymalStream = this.getClass().getResourceAsStream("ServiceNoApp.yml");
        Yaml yaml = new Yaml();
        yaml.loadAs
        yaml.load(ymalStream);
        String serviceConfigRaw = "---\n" +
                "scope: service/application\n" +
                "key: serviceKey/appName\n" +
                "configs:\n" +
                " - addresses:[ip1, ip2]\n" +
                "   apps: [app1, app2]\n" +
                "   services: [s1, s2]\n" +
                "   side: provider\n" +
                "   rules:\n" +
                "    threadpool:\n" +
                "     size:\n" +
                "     core:\n" +
                "     queue:\n" +
                "    cluster:\n" +
                "     loadbalance:\n" +
                "     cluster:\n" +
                "    config:\n" +
                "     timeout:\n" +
                "     weight:\n" +
                " - addresses: [ip1, ip2]\n" +
                "   rules:\n" +
                "    threadpool:\n" +
                "     size:\n" +
                "     core:\n" +
                "     queue:\n" +
                "    cluster:\n" +
                "     loadbalance:\n" +
                "     cluster:\n" +
                "    config:\n" +
                "     timeout:\n" +
                "     weight:\n" +
                "   apps: [app1, app2]\n" +
                "   services: [s1, s2]\n" +
                "   side: provider\n" +
                "...";

    }

    @Test
    public void parseConfiguratorsServiceMultiAppsTest() {
        String serviceConfigRaw = "";
    }

    @Test
    public void parseConfiguratorsServiceAnyTest() {
        String serviceConfigRaw = "";
    }

    @Test
    public void parseConfiguratorsServiceNoRuleTest() {
        String serviceConfigRaw = "";
    }
}
