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
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.router.state.BitList;

import java.util.HashSet;
import java.util.Set;

public class DestinationSubset<T> {

    public DestinationSubset(String clusterName) {
        this.clusterName = clusterName;
    }

    private final String clusterName;

    private Set<Endpoint> endpoints = new HashSet<>();

    private BitList<Invoker<T>> invokers;

    public String getClusterName() {
        return clusterName;
    }

    public Set<Endpoint> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(Set<Endpoint> endpoints) {
        this.endpoints = endpoints;

    }

    public BitList<Invoker<T>> getInvokers() {
        return invokers;
    }

    public void setInvokers(BitList<Invoker<T>> invokers) {
        this.invokers = invokers;
    }
}
