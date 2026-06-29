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
package org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.match;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddressMatchTest {

    @Test
    void cirdMatchIpv4AddressWithPort() {
        AddressMatch addressMatch = new AddressMatch();
        addressMatch.setCird("192.168.1.*:90");

        assertTrue(addressMatch.isMatch("192.168.1.63:90"));
        assertFalse(addressMatch.isMatch("192.168.1.63:80"));
    }

    @Test
    void cirdMatchIpv4AddressWithoutPort() {
        AddressMatch addressMatch = new AddressMatch();
        addressMatch.setCird("192.168.1.*");

        assertTrue(addressMatch.isMatch("192.168.1.63"));
    }

    @Test
    void cirdMatchExactAddress() {
        AddressMatch addressMatch = new AddressMatch();
        addressMatch.setCird("192.168.1.63:90");

        assertTrue(addressMatch.isMatch("192.168.1.63:90"));
    }

    @Test
    void cirdMatchIpv6Address() {
        AddressMatch addressMatch = new AddressMatch();
        addressMatch.setCird("234e:0:4567:0:0:0:3d:*");

        assertTrue(addressMatch.isMatch("234e:0:4567::3d:ff"));
    }
}
