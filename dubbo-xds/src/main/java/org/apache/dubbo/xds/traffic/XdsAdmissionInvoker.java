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

import org.apache.dubbo.rpc.CancellationContext;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.model.MethodDescriptor.RpcType;
import org.apache.dubbo.rpc.protocol.InvokerWrapper;
import org.apache.dubbo.rpc.protocol.tri.TripleInvoker;
import org.apache.dubbo.xds.client.XdsResourceGraph.Snapshot;
import org.apache.dubbo.xds.registry.XdsEndpoint;
import org.apache.dubbo.xds.traffic.XdsCircuitBreaker.Permit;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.Cluster;

/** Applies admission at dispatch, including point-to-point native MetadataService calls. */
public final class XdsAdmissionInvoker<T> extends InvokerWrapper<T> {
    private final XdsEndpoint endpoint;
    private final Set<CancellationContext> ownedStreams = ConcurrentHashMap.newKeySet();

    public XdsAdmissionInvoker(Invoker<T> delegate, XdsEndpoint endpoint) {
        super(delegate, delegate.getUrl());
        this.endpoint = endpoint;
    }

    @Override
    public Result invoke(Invocation invocation) {
        Snapshot snapshot = endpoint.getSnapshot();
        Cluster cluster = snapshot == null
                ? null
                : snapshot.getClusters().get(endpoint.getCluster().getName());
        if (cluster == null) {
            throw TriRpcStatus.UNAVAILABLE
                    .withDescription("Native xDS cluster has been removed")
                    .asException();
        }
        XdsLoadStats stats = endpoint.getSecurity().getLoadStats();
        if (invocation.get(XdsRouteSelection.ATTRIBUTE) == null) XdsDropPolicy.apply(snapshot, cluster.getName());
        Permit permit;
        try {
            permit = XdsCircuitBreaker.acquire(
                    endpoint.getSecurity().getBootstrap().getFingerprint(), cluster);
        } catch (XdsCircuitBreaker.DropException dropped) {
            stats.circuitDropped(cluster);
            throw dropped;
        }
        XdsLoadStats.Attempt attempt =
                stats.started(cluster, endpoint.getLocality().getLocality());
        CancellationContext streaming = null;
        try {
            if (TripleInvoker.resolveMethodDescriptor(getUrl(), invocation).getRpcType() != RpcType.UNARY) {
                streaming = (CancellationContext) invocation.get(TripleInvoker.STREAM_CANCELLATION_ATTRIBUTE);
                if (streaming == null) {
                    streaming = new CancellationContext();
                    invocation.put(TripleInvoker.STREAM_CANCELLATION_ATTRIBUTE, streaming);
                    CancellationContext owned = streaming;
                    ownedStreams.add(owned);
                    owned.addListener(ignored -> ownedStreams.remove(owned));
                }
                CancellationContext terminal = streaming;
                streaming.addListener(ignored -> {
                    attempt.finished(terminal.getCancellationCause());
                    permit.close();
                });
            }
            CancellationContext lifecycle = streaming;
            Result result = super.invoke(invocation);
            result.whenCompleteWithContext((response, error) -> {
                if (lifecycle == null) {
                    attempt.finished(error == null && response != null ? response.getException() : error);
                    permit.close();
                } else if (error != null || (response != null && response.hasException())) {
                    lifecycle.cancel(error == null ? response.getException() : error);
                }
            });
            return result;
        } catch (RuntimeException | Error error) {
            if (streaming != null) streaming.cancel(error);
            attempt.finished(error);
            permit.close();
            throw error;
        }
    }

    @Override
    public void destroy() {
        ownedStreams.forEach(stream -> stream.cancel(TriRpcStatus.CANCELLED
                .withDescription("Native xDS invoker destroyed")
                .asException()));
        super.destroy();
    }
}
