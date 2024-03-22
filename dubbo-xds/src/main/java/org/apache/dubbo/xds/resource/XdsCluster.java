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
package org.apache.dubbo.xds.resource;

import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.router.state.BitList;

import java.util.List;

public class XdsCluster<T> {
    private String name;

    private String lbPolicy;

    private List<XdsEndpoint> xdsEndpoints;

    public BitList<Invoker<T>> getInvokers() {
        return invokers;
    }

    public void setInvokers(BitList<Invoker<T>> invokers) {
        this.invokers = invokers;
    }

    private BitList<Invoker<T>> invokers;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLbPolicy() {
        return lbPolicy;
    }

    public void setLbPolicy(String lbPolicy) {
        this.lbPolicy = lbPolicy;
    }

    public List<XdsEndpoint> getXdsEndpoints() {
        return xdsEndpoints;
    }

    public void setXdsEndpoints(List<XdsEndpoint> xdsEndpoints) {
        this.xdsEndpoints = xdsEndpoints;
    }
}
