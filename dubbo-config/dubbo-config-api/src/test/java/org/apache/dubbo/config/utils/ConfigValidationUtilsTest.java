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
package org.apache.dubbo.config.utils;

import org.apache.dubbo.config.AbstractInterfaceConfig;
import org.apache.dubbo.config.api.Greeting;
import org.apache.dubbo.config.mock.GreetingMock1;
import org.apache.dubbo.config.mock.GreetingMock2;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class ConfigValidationUtilsTest {


    @Test
    public void checkMock1() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            InterfaceConfig interfaceConfig = new InterfaceConfig();
            interfaceConfig.setMock("return {a, b}");
            ConfigValidationUtils.checkMock(Greeting.class, interfaceConfig);

        });
    }

    @Test
    public void checkMock2() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            InterfaceConfig interfaceConfig = new InterfaceConfig();
            interfaceConfig.setMock(GreetingMock1.class.getName());
            ConfigValidationUtils.checkMock(Greeting.class, interfaceConfig);
        });
    }

    @Test
    public void checkMock3() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            InterfaceConfig interfaceConfig = new InterfaceConfig();
            interfaceConfig.setMock(GreetingMock2.class.getName());
            ConfigValidationUtils.checkMock(Greeting.class, interfaceConfig);
        });
    }

    public static class InterfaceConfig extends AbstractInterfaceConfig {

    }

}
