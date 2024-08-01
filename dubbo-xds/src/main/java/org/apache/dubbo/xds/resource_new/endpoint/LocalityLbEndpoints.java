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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LocalityLbEndpoints {

    private final List<LbEndpoint> endpoints;

    private final int localityWeight;

    private final int priority;

    public LocalityLbEndpoints(List<LbEndpoint> endpoints, int localityWeight, int priority) {
        if (endpoints == null) {
            throw new NullPointerException("Null endpoints");
        }
        this.endpoints = Collections.unmodifiableList(new ArrayList<>(endpoints));
        this.localityWeight = localityWeight;
        this.priority = priority;
    }

    List<LbEndpoint> endpoints() {
        return endpoints;
    }

    public int localityWeight() {
        return localityWeight;
    }

    public int priority() {
        return priority;
    }

    public String toString() {
        return "LocalityLbEndpoints{" + "endpoints=" + endpoints + ", " + "localityWeight=" + localityWeight + ", "
                + "priority=" + priority + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof LocalityLbEndpoints) {
            LocalityLbEndpoints that = (LocalityLbEndpoints) o;
            return this.endpoints.equals(that.endpoints())
                    && this.localityWeight == that.localityWeight()
                    && this.priority == that.priority();
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= endpoints.hashCode();
        h$ *= 1000003;
        h$ ^= localityWeight;
        h$ *= 1000003;
        h$ ^= priority;
        return h$;
    }
}
