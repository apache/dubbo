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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * {@link CompositeConfiguration}
 */
public class CompositeConfigurationTest {

    @Test
    public void test() {
        InmemoryConfiguration inmemoryConfiguration1 = new InmemoryConfiguration();
        InmemoryConfiguration inmemoryConfiguration2 = new InmemoryConfiguration();
        InmemoryConfiguration inmemoryConfiguration3 = new InmemoryConfiguration();
        CompositeConfiguration configuration = new CompositeConfiguration(new Configuration[]{inmemoryConfiguration1});
        configuration.addConfiguration(inmemoryConfiguration2);
        configuration.addConfigurationFirst(inmemoryConfiguration3);

        inmemoryConfiguration1.addProperty("k", "v1");
        inmemoryConfiguration2.addProperty("k", "v2");
        inmemoryConfiguration3.addProperty("k", "v3");

        Assertions.assertEquals(configuration.getInternalProperty("k"), "v3");
    }
}
