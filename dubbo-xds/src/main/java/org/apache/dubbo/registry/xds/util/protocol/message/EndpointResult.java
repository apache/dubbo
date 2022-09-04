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
package org.apache.dubbo.registry.xds.util.protocol.message;

import org.apache.dubbo.common.utils.ConcurrentHashSet;

import java.util.Objects;
import java.util.Set;

public class EndpointResult {
    private Set<Endpoint> endpoints;

    public EndpointResult() {
        this.endpoints = new ConcurrentHashSet<>();
    }

    public EndpointResult(Set<Endpoint> endpoints) {
        this.endpoints = endpoints;
    }

    public Set<Endpoint> getEndpoints() {
        return endpoints;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EndpointResult that = (EndpointResult) o;
        return Objects.equals(endpoints, that.endpoints);
    }

    @Override
    public int hashCode() {
        return Objects.hash(endpoints);
    }

    @Override
    public String toString() {
        return "EndpointResult{" +
            "endpoints=" + endpoints +
            '}';
    }
}
