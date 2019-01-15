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

import org.junit.Assert;
import org.junit.Test;

public class VersionTest {

    @Test
    public void testGetProtocolVersion() {
        Assert.assertEquals(Version.getProtocolVersion(), Version.DEFAULT_DUBBO_PROTOCOL_VERSION);
    }

    @Test
    public void testSupportResponseAttachment() {
        Assert.assertTrue(Version.isSupportResponseAttachment("2.0.2"));
        Assert.assertTrue(Version.isSupportResponseAttachment("2.0.3"));
        Assert.assertFalse(Version.isSupportResponseAttachment("2.0.0"));
    }

    @Test
    public void testGetIntVersion() {
        Assert.assertEquals(2060100, Version.getIntVersion("2.6.1"));
        Assert.assertEquals(2060101, Version.getIntVersion("2.6.1.1"));
        Assert.assertEquals(2070001, Version.getIntVersion("2.7.0.1"));
        Assert.assertEquals(2070000, Version.getIntVersion("2.7.0"));
    }

    @Test
    public void testIsFramework270OrHigher() {
        Assert.assertTrue(Version.isFramework270OrHigher("2.7.0"));
        Assert.assertTrue(Version.isFramework270OrHigher("2.7.0.1"));
        Assert.assertTrue(Version.isFramework270OrHigher("2.7.0.2"));
        Assert.assertTrue(Version.isFramework270OrHigher("2.8.0"));
        Assert.assertFalse(Version.isFramework270OrHigher("2.6.3"));
        Assert.assertFalse(Version.isFramework270OrHigher("2.6.3.1"));
    }

    @Test
    public void testIsFramework263OrHigher() {
        Assert.assertTrue(Version.isFramework263OrHigher("2.7.0"));
        Assert.assertTrue(Version.isFramework263OrHigher("2.7.0.1"));
        Assert.assertTrue(Version.isFramework263OrHigher("2.6.4"));
        Assert.assertFalse(Version.isFramework263OrHigher("2.6.2"));
        Assert.assertFalse(Version.isFramework263OrHigher("2.6.1.1"));
        Assert.assertTrue(Version.isFramework263OrHigher("2.6.3"));
    }
}
