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
package org.apache.dubbo.xds.traffic;

import org.apache.dubbo.remoting.api.connection.ConnectionStateProvider;
import org.apache.dubbo.remoting.api.connection.ConnectionStateProvider.State;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.protocol.InvokerWrapper;
import org.apache.dubbo.rpc.protocol.tri.TripleInvoker;
import org.apache.dubbo.xds.client.XdsResourceGraph.Snapshot;
import org.apache.dubbo.xds.registry.XdsEndpoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** MetadataService executes fault policy but remains pinned to the discovered instance. */
public final class XdsMetadataFaultInvoker<T> extends InvokerWrapper<T> {
    private final XdsEndpoint endpoint;
    private XdsFaultExecutor faults;
    private boolean closed;

    public XdsMetadataFaultInvoker(Invoker<T> invoker, XdsEndpoint endpoint) {
        super(invoker, invoker.getUrl());
        this.endpoint = endpoint;
    }

    @Override
    public Result invoke(Invocation invocation) {
        Snapshot snapshot = endpoint.getSnapshot();
        if (snapshot == null)
            throw TriRpcStatus.UNAVAILABLE
                    .withDescription("Metadata xDS snapshot unavailable")
                    .asException();
        Map<String, String> matchHeaders = new LinkedHashMap<>();
        Map<String, String> faultHeaders = new LinkedHashMap<>();
        TripleInvoker.createRoutingHeaders(getUrl(), invocation).forEach(entry -> {
            String key = entry.getKey().toString().toLowerCase(Locale.ROOT);
            String value = entry.getValue().toString();
            matchHeaders.merge(key, value, (first, next) -> first + "," + next);
            faultHeaders.putIfAbsent(key, value);
        });
        XdsRouteTable.Match route = XdsRouteTable.select(snapshot, matchHeaders);
        Collection<XdsFaultPolicy> policies;
        if (route == null) {
            policies = new ArrayList<>();
            if (snapshot.getFaultFilters() != null)
                snapshot.getFaultFilters().getPolicies().values().forEach(policy -> {
                    if (policy.hasActions()) policies.add(policy);
                });
        } else policies = route.getFaultPolicies(snapshot, endpoint.getCluster().getName());
        XdsCallBudget.begin(getUrl(), invocation)
                .limit(
                        invocation,
                        route == null
                                ? snapshot.getDefaultTimeoutNanos()
                                : route.getTimeoutNanos(snapshot.getDefaultTimeoutNanos()));
        return executor().invoke(getUrl(), invocation, policies, faultHeaders, attempt -> {
            Object provider = getUrl().getAttribute(ConnectionStateProvider.ATTRIBUTE);
            if (provider instanceof ConnectionStateProvider) {
                State state = ((ConnectionStateProvider) provider).getConnectivityState();
                if (state == State.CONNECTING || state == State.IDLE) return null;
                if (state != State.READY)
                    throw TriRpcStatus.UNAVAILABLE
                            .withDescription("Metadata transport unavailable")
                            .asException();
            }
            return super.invoke(attempt);
        });
    }

    private synchronized XdsFaultExecutor executor() {
        if (closed) throw new IllegalStateException("Metadata fault invoker has been destroyed");
        if (faults == null) faults = new XdsFaultExecutor();
        return faults;
    }

    @Override
    public synchronized void destroy() {
        closed = true;
        if (faults != null) faults.close();
        super.destroy();
    }
}
