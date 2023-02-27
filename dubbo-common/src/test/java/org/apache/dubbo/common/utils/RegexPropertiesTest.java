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
package org.apache.dubbo.common.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RegexPropertiesTest {
    @Test
    void testGetProperty(){
        RegexProperties regexProperties = new RegexProperties();
        regexProperties.setProperty("org.apache.dubbo.provider.*", "http://localhost:20880");
        regexProperties.setProperty("org.apache.dubbo.provider.config.*", "http://localhost:30880");
        regexProperties.setProperty("org.apache.dubbo.provider.config.demo", "http://localhost:40880");
        regexProperties.setProperty("org.apache.dubbo.consumer.*.demo", "http://localhost:50880");
        regexProperties.setProperty("*.service", "http://localhost:60880");

        Assertions.assertEquals("http://localhost:20880", regexProperties.getProperty("org.apache.dubbo.provider.cluster"));
        Assertions.assertEquals("http://localhost:30880", regexProperties.getProperty("org.apache.dubbo.provider.config.cluster"));
        Assertions.assertEquals("http://localhost:40880", regexProperties.getProperty("org.apache.dubbo.provider.config.demo"));
        Assertions.assertEquals("http://localhost:50880", regexProperties.getProperty("org.apache.dubbo.consumer.service.demo"));
        Assertions.assertEquals("http://localhost:60880", regexProperties.getProperty("org.apache.dubbo.service"));
    }
}