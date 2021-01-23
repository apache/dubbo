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

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RouteResult {
    private final Map<String, Set<String>> domainMap;

    public RouteResult() {
        this.domainMap = new ConcurrentHashMap<>();
    }

    public RouteResult(Map<String, Set<String>> domainMap) {
        this.domainMap = domainMap;
    }

    public boolean isNotEmpty() {
        return !domainMap.isEmpty();
    }

    public Set<String> searchDomain(String domain) {
        return domainMap.getOrDefault(domain, new ConcurrentHashSet<>());
    }

    public Set<String> getDomains() {
        return Collections.unmodifiableSet(domainMap.keySet());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RouteResult that = (RouteResult) o;
        return Objects.equals(domainMap, that.domainMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(domainMap);
    }

    @Override
    public String toString() {
        return "RouteResult{" +
                "domainMap=" + domainMap +
                '}';
    }
}
