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
package org.apache.dubbo.xds.resource.common;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class CidrRange {

    private final InetAddress addressPrefix;

    private final int prefixLen;

    CidrRange(InetAddress addressPrefix, int prefixLen) {
        if (addressPrefix == null) {
            throw new NullPointerException("Null addressPrefix");
        }
        this.addressPrefix = addressPrefix;
        this.prefixLen = prefixLen;
    }

    public InetAddress getAddressPrefix() {
        return addressPrefix;
    }

    public int getPrefixLen() {
        return prefixLen;
    }

    @Override
    public String toString() {
        return "CidrRange{" + "addressPrefix=" + addressPrefix + ", " + "prefixLen=" + prefixLen + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof CidrRange) {
            CidrRange that = (CidrRange) o;
            return this.addressPrefix.equals(that.getAddressPrefix()) && this.prefixLen == that.getPrefixLen();
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= addressPrefix.hashCode();
        h$ *= 1000003;
        h$ ^= prefixLen;
        return h$;
    }

    public static CidrRange create(String addressPrefix, int prefixLen) throws UnknownHostException {
        return new CidrRange(InetAddress.getByName(addressPrefix), prefixLen);
    }
}
