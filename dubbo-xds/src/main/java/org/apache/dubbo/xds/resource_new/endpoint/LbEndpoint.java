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
package org.apache.dubbo.xds.resource_new.endpoint;

import org.apache.dubbo.common.url.component.URLAddress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LbEndpoint {

    private final List<URLAddress> addresses;

    private final int loadBalancingWeight;

    private final boolean isHealthy;

    public LbEndpoint(List<URLAddress> addresses, int loadBalancingWeight, boolean isHealthy) {
        if (addresses == null) {
            throw new NullPointerException("Null addresses");
        }
        this.addresses = Collections.unmodifiableList(new ArrayList<>(addresses));
        this.loadBalancingWeight = loadBalancingWeight;
        this.isHealthy = isHealthy;
    }

    public List<URLAddress> getAddresses() {
        return addresses;
    }

    public int getLoadBalancingWeight() {
        return loadBalancingWeight;
    }

    public boolean isHealthy() {
        return isHealthy;
    }

    @Override
    public String toString() {
        return "LbEndpoint{" + "addresses=" + addresses + ", " + "loadBalancingWeight=" + loadBalancingWeight + ", "
                + "isHealthy=" + isHealthy + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof LbEndpoint) {
            LbEndpoint that = (LbEndpoint) o;
            return this.addresses.equals(that.getAddresses())
                    && this.loadBalancingWeight == that.getLoadBalancingWeight()
                    && this.isHealthy == that.isHealthy();
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= addresses.hashCode();
        h$ *= 1000003;
        h$ ^= loadBalancingWeight;
        h$ *= 1000003;
        h$ ^= isHealthy ? 1231 : 1237;
        return h$;
    }
}
