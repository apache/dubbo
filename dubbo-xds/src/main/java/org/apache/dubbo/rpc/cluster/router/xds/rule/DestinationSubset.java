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
package org.apache.dubbo.rpc.cluster.router.xds.rule;

import org.apache.dubbo.registry.xds.util.protocol.message.Endpoint;

import java.util.Set;
import java.util.stream.Collectors;

public class DestinationSubset<T> {

    private Set<String> endpointSet;

    public DestinationSubset(String clusterName) {
        this.clusterName = clusterName;
    }

    private final String clusterName;

    private Set<Endpoint> endpoints;


    public String getClusterName() {
        return clusterName;
    }

    public Set<String> getEndpointAddress() {
        return endpointSet;
    }

    public Set<Endpoint> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(Set<Endpoint> endpoints) {
        this.endpoints = endpoints;
        endpointSet = endpoints.stream().map(end ->
                end.getPortValue() <= 0 ? end.getAddress() : end.getAddress() + ':' + end.getPortValue())
            .collect(Collectors.toSet());
    }

}
