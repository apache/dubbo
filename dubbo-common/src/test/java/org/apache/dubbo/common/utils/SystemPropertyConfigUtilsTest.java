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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

public class SystemPropertyConfigUtilsTest {

    @Test
    public void testGetSystemProperty() {
        SystemPropertyConfigUtils.setSystemProperty("dubbo.migration.file", "migration.xml");
        String value = SystemPropertyConfigUtils.getSystemProperty("dubbo.migration.file");
        assertEquals(value, "migration.xml");
        SystemPropertyConfigUtils.clearSystemProperty("dubbo.migration.file");
    }

    @Test
    public void testGetSystemPropertyNotExist() {
        assertThrowsExactly(
                IllegalStateException.class, () -> SystemPropertyConfigUtils.getSystemProperty("dubbo.not.exist"));
    }

    @Test
    public void testGetSystemPropertyWithDefaultValue() {
        String value = SystemPropertyConfigUtils.getSystemProperty("dubbo.migration.file", "migration.xml");
        assertEquals(value, "migration.xml");
    }

    @Test
    public void testSetSystemProperty() {
        SystemPropertyConfigUtils.setSystemProperty("dubbo.migration.file", "migration.xml");
        String expectValue = SystemPropertyConfigUtils.getSystemProperty("dubbo.migration.file");
        assertEquals(expectValue, "migration.xml");
        SystemPropertyConfigUtils.clearSystemProperty("dubbo.migration.file");
    }

    @Test
    public void testClearSystemProperty() {
        SystemPropertyConfigUtils.setSystemProperty("dubbo.migration.file", "migration33.xml");
        SystemPropertyConfigUtils.clearSystemProperty("dubbo.migration.file");
        String expectValue = SystemPropertyConfigUtils.getSystemProperty("dubbo.migration.file");
        assertNull(expectValue);
    }
}
