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
package org.apache.dubbo.rpc.cluster;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.cluster.configurator.absent.AbsentConfigurator;
import org.apache.dubbo.rpc.cluster.configurator.override.OverrideConfigurator;
import org.apache.dubbo.rpc.cluster.configurator.parser.ConfigParser;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * {@link Configurator}
 */
class ConfiguratorTest {

    @Test
    void test() {

        Optional<List<Configurator>> emptyOptional = Configurator.toConfigurators(Collections.emptyList());
        Assertions.assertEquals(Optional.empty(), emptyOptional);

        String configData = "[\"override://0.0.0.0/com.xx.Service?category=configurators&timeout=6666&disabled=true&dynamic=false&enabled=true&group=dubbo&priority=2&version=1.0\"" +
            ", \"absent://0.0.0.0/com.xx.Service?category=configurators&timeout=6666&disabled=true&dynamic=false&enabled=true&group=dubbo&priority=1&version=1.0\" ]";
        List<URL> urls = ConfigParser.parseConfigurators(configData);
        Optional<List<Configurator>> optionalList = Configurator.toConfigurators(urls);
        Assertions.assertTrue(optionalList.isPresent());
        List<Configurator> configurators = optionalList.get();
        Assertions.assertEquals(configurators.size(), 2);
        // The hosts of AbsentConfigurator and OverrideConfigurator are equal, but the priority of OverrideConfigurator is higher
        Assertions.assertTrue(configurators.get(0) instanceof AbsentConfigurator);
        Assertions.assertTrue(configurators.get(1) instanceof OverrideConfigurator);
    }

}
