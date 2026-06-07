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

import org.apache.dubbo.common.utils.PojoUtils;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddressMatchTest {

    @Test
    void cidrMatchIpv4AddressWithPort() {
        AddressMatch addressMatch = new AddressMatch();
        addressMatch.setCidr("192.168.1.*:90");

        assertTrue(addressMatch.isMatch("192.168.1.63:90"));
        assertFalse(addressMatch.isMatch("192.168.1.63:80"));
    }

    @Test
    void cidrMatchIpv4AddressWithoutPort() {
        AddressMatch addressMatch = new AddressMatch();
        addressMatch.setCidr("192.168.1.*");

        assertTrue(addressMatch.isMatch("192.168.1.63"));
    }

    @Test
    void cidrMatchExactAddress() {
        AddressMatch addressMatch = new AddressMatch();
        addressMatch.setCidr("192.168.1.63:90");

        assertTrue(addressMatch.isMatch("192.168.1.63:90"));
    }

    @Test
    void cidrMatchIpv6Address() {
        AddressMatch addressMatch = new AddressMatch();
        addressMatch.setCidr("234e:0:4567:0:0:0:3d:*");

        assertTrue(addressMatch.isMatch("234e:0:4567::3d:ff"));
    }

    @Test
    void cidrMatchInvalidAddressReturnsFalse() {
        AddressMatch addressMatch = new AddressMatch();
        addressMatch.setCidr("192.168.1.*");

        assertFalse(addressMatch.isMatch("invalid host"));
    }

    @Test
    @SuppressWarnings("deprecation")
    void deprecatedCirdAccessorsRemainCompatible() {
        AddressMatch addressMatch = new AddressMatch();
        addressMatch.setCird("192.168.1.*:90");

        assertTrue(addressMatch.isMatch("192.168.1.63:90"));
        assertEquals(addressMatch.getCidr(), addressMatch.getCird());
    }

    @Test
    @SuppressWarnings("deprecation")
    void cidrAndDeprecatedCirdAccessorsShareTheSameValue() {
        AddressMatch addressMatch = new AddressMatch();
        addressMatch.setCidr("192.168.1.*:90");
        addressMatch.setCird("10.0.0.*:20880");

        assertEquals("10.0.0.*:20880", addressMatch.getCidr());
        assertEquals(addressMatch.getCidr(), addressMatch.getCird());
    }

    @Test
    void cidrFieldCanBeMappedToPojo() throws ReflectiveOperationException {
        Map<String, Object> map = new HashMap<>();
        map.put("cidr", "192.168.1.*:90");

        AddressMatch addressMatch = PojoUtils.mapToPojo(map, AddressMatch.class);

        assertTrue(addressMatch.isMatch("192.168.1.63:90"));
    }

    @Test
    void deprecatedCirdFieldCanBeMappedToPojo() throws ReflectiveOperationException {
        Map<String, Object> map = new HashMap<>();
        map.put("cird", "192.168.1.*:90");

        AddressMatch addressMatch = PojoUtils.mapToPojo(map, AddressMatch.class);

        assertTrue(addressMatch.isMatch("192.168.1.63:90"));
    }
}
