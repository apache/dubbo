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
package org.apache.dubbo.common.version;


import org.apache.dubbo.common.Version;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class VersionTest {

    @Test
    public void testGetProtocolVersion() {
        Assertions.assertEquals(Version.getProtocolVersion(), Version.DEFAULT_DUBBO_PROTOCOL_VERSION);
    }

    @Test
    public void testSupportResponseAttachment() {
        Assertions.assertTrue(Version.isSupportResponseAttachment("2.0.2"));
        Assertions.assertTrue(Version.isSupportResponseAttachment("2.0.3"));
        Assertions.assertTrue(Version.isSupportResponseAttachment("2.0.99"));

        Assertions.assertFalse(Version.isSupportResponseAttachment("2.1.0"));
        Assertions.assertFalse(Version.isSupportResponseAttachment("2.0.0"));
        Assertions.assertFalse(Version.isSupportResponseAttachment("1.0.0"));
        Assertions.assertFalse(Version.isSupportResponseAttachment("3.0.0"));
        Assertions.assertFalse(Version.isSupportResponseAttachment("2.6.6-stable"));
        Assertions.assertFalse(Version.isSupportResponseAttachment("2.6.6.RC1"));
        Assertions.assertFalse(Version.isSupportResponseAttachment("2.0.contains"));
        Assertions.assertFalse(Version.isSupportResponseAttachment("version.string"));
        Assertions.assertFalse(Version.isSupportResponseAttachment("prefix2.0"));
    }

    @Test
    public void testGetIntVersion() {
        Assertions.assertEquals(2060100, Version.getIntVersion("2.6.1"));
        Assertions.assertEquals(2060101, Version.getIntVersion("2.6.1.1"));
        Assertions.assertEquals(2070001, Version.getIntVersion("2.7.0.1"));
        Assertions.assertEquals(2070000, Version.getIntVersion("2.7.0"));
        Assertions.assertEquals(Version.HIGHEST_PROTOCOL_VERSION, Version.getIntVersion("2.0.99"));
        Assertions.assertEquals(2070000, Version.getIntVersion("2.7.0.RC1"));
        Assertions.assertEquals(2070000, Version.getIntVersion("2.7.0-SNAPSHOT"));
        Assertions.assertEquals(3000000, Version.getIntVersion("3.0.0-SNAPSHOT"));
        Assertions.assertEquals(3010000, Version.getIntVersion("3.1.0"));
    }

    @Test
    public void testCompare() {
        Assertions.assertEquals(0, Version.compare("3.0.0", "3.0.0"));
        Assertions.assertEquals(0, Version.compare("3.0.0-SNAPSHOT", "3.0.0"));
        Assertions.assertEquals(1, Version.compare("3.0.0.1", "3.0.0"));
        Assertions.assertEquals(1, Version.compare("3.1.0", "3.0.0"));
        Assertions.assertEquals(1, Version.compare("3.1.2.3", "3.0.0"));
        Assertions.assertEquals(-1, Version.compare("2.9.9.9", "3.0.0"));
        Assertions.assertEquals(-1, Version.compare("2.6.3.1", "3.0.0"));
    }

    @Test
    public void testIsFramework270OrHigher() {
        Assertions.assertTrue(Version.isRelease270OrHigher("2.7.0"));
        Assertions.assertTrue(Version.isRelease270OrHigher("2.7.0.1"));
        Assertions.assertTrue(Version.isRelease270OrHigher("2.7.0.2"));
        Assertions.assertTrue(Version.isRelease270OrHigher("2.8.0"));
        Assertions.assertFalse(Version.isRelease270OrHigher("2.6.3"));
        Assertions.assertFalse(Version.isRelease270OrHigher("2.6.3.1"));
    }

    @Test
    public void testIsFramework263OrHigher() {
        Assertions.assertTrue(Version.isRelease263OrHigher("2.7.0"));
        Assertions.assertTrue(Version.isRelease263OrHigher("2.7.0.1"));
        Assertions.assertTrue(Version.isRelease263OrHigher("2.6.4"));
        Assertions.assertFalse(Version.isRelease263OrHigher("2.6.2"));
        Assertions.assertFalse(Version.isRelease263OrHigher("2.6.2.1"));
        Assertions.assertFalse(Version.isRelease263OrHigher("2.6.1.1"));
        Assertions.assertTrue(Version.isRelease263OrHigher("2.6.3"));
        Assertions.assertTrue(Version.isRelease263OrHigher("2.6.3.0"));
    }
}
