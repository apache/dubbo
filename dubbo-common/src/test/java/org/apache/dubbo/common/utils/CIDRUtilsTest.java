/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import java.net.UnknownHostException;

class CIDRUtilsTest {

    @Test
    void testIpv4() throws UnknownHostException {
        CIDRUtils cidrUtils = new CIDRUtils("192.168.1.0/26");
        Assertions.assertTrue(cidrUtils.isInRange("192.168.1.63"));
        Assertions.assertFalse(cidrUtils.isInRange("192.168.1.65"));

        cidrUtils = new CIDRUtils("192.168.1.192/26");
        Assertions.assertTrue(cidrUtils.isInRange("192.168.1.199"));
        Assertions.assertFalse(cidrUtils.isInRange("192.168.1.190"));
    }

    @Test
    void testIpv6() throws UnknownHostException {
        CIDRUtils cidrUtils = new CIDRUtils("234e:0:4567::3d/64");
        Assertions.assertTrue(cidrUtils.isInRange("234e:0:4567::3e"));
        Assertions.assertTrue(cidrUtils.isInRange("234e:0:4567::ffff:3e"));
        Assertions.assertFalse(cidrUtils.isInRange("234e:1:4567::3d"));
        Assertions.assertFalse(cidrUtils.isInRange("234e:0:4567:1::3d"));

        cidrUtils = new CIDRUtils("3FFE:FFFF:0:CC00::/54");
        Assertions.assertTrue(cidrUtils.isInRange("3FFE:FFFF:0:CC00::dd"));
        Assertions.assertTrue(cidrUtils.isInRange("3FFE:FFFF:0:CC00:0000:eeee:0909:dd"));
        Assertions.assertTrue(cidrUtils.isInRange("3FFE:FFFF:0:CC0F:0000:eeee:0909:dd"));

        Assertions.assertFalse(cidrUtils.isInRange("3EFE:FFFE:0:C107::dd"));
        Assertions.assertFalse(cidrUtils.isInRange("1FFE:FFFE:0:CC00::dd"));
    }
}