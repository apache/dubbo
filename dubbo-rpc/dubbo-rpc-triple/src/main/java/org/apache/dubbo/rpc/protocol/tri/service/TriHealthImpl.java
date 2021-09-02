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

package org.apache.dubbo.rpc.protocol.tri.service;

import grpc.health.v1.Health;
import grpc.health.v1.HealthCheckRequest;
import grpc.health.v1.HealthCheckResponse;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.protocol.tri.GrpcStatus;
import org.apache.dubbo.rpc.protocol.tri.TripleRpcException;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TriHealthImpl implements Health {

    private static final Logger logger = Logger.getLogger(TriHealthImpl.class.getName());

    // Due to the latency of rpc calls, synchronization of the map does not help with consistency.
    // However, need use ConcurrentHashMap to allow concurrent reading by check().
    private final Map<String, HealthCheckResponse.ServingStatus> statusMap = new ConcurrentHashMap<>();

    private final Object watchLock = new Object();

    // Indicates if future status changes should be ignored.
    private boolean terminal;

    // Technically a Multimap<String, StreamObserver<HealthCheckResponse>>.  The Boolean value is not
    // used.  The StreamObservers need to be kept in a identity-equality set, to make sure
    // user-defined equals() doesn't confuse our book-keeping of the StreamObservers.  Constructing
    // such Multimap would require extra lines and the end result is not significantly simpler, thus I
    // would rather not have the Guava collections dependency.
    private final HashMap<String, IdentityHashMap<StreamObserver<HealthCheckResponse>, Boolean>>
        watchers = new HashMap<>();

    public TriHealthImpl() {
        // Copy of what Go and C++ do.
        statusMap.put(HealthStatusManager.SERVICE_NAME_ALL_SERVICES, HealthCheckResponse.ServingStatus.SERVING);
    }

    @Override
    public HealthCheckResponse check(HealthCheckRequest request) {
        HealthCheckResponse.ServingStatus status = statusMap.get(request.getService());
        if (status != null) {
            return HealthCheckResponse.newBuilder().setStatus(status).build();
        }
        throw new TripleRpcException(GrpcStatus.fromCode(GrpcStatus.Code.NOT_FOUND)
            .withDescription("unknown service " + request.getService()));
    }

    @Override
    public void watch(HealthCheckRequest request, StreamObserver<HealthCheckResponse> responseObserver) {
        final String service = request.getService();
        synchronized (watchLock) {
            HealthCheckResponse.ServingStatus status = statusMap.get(service);
            responseObserver.onNext(getResponseForWatch(status));
            IdentityHashMap<StreamObserver<HealthCheckResponse>, Boolean> serviceWatchers =
                watchers.get(service);
            if (serviceWatchers == null) {
                serviceWatchers = new IdentityHashMap<>();
                watchers.put(service, serviceWatchers);
            }
            serviceWatchers.put(responseObserver, Boolean.TRUE);
        }
        // todo add client cancel listener
    }

    void setStatus(String service, HealthCheckResponse.ServingStatus status) {
        synchronized (watchLock) {
            if (terminal) {
                logger.log(Level.FINE, "Ignoring status {} for {}", new Object[]{status, service});
                return;
            }
            setStatusInternal(service, status);
        }
    }

    private void setStatusInternal(String service, HealthCheckResponse.ServingStatus status) {
        HealthCheckResponse.ServingStatus prevStatus = statusMap.put(service, status);
        if (prevStatus != status) {
            notifyWatchers(service, status);
        }
    }

    void clearStatus(String service) {
        synchronized (watchLock) {
            if (terminal) {
                logger.log(Level.FINE, "Ignoring status clearing for {}", new Object[]{service});
                return;
            }
            HealthCheckResponse.ServingStatus prevStatus = statusMap.remove(service);
            if (prevStatus != null) {
                notifyWatchers(service, null);
            }
        }
    }

    void enterTerminalState() {
        synchronized (watchLock) {
            if (terminal) {
                logger.log(Level.WARNING, "Already terminating", new RuntimeException());
                return;
            }
            terminal = true;
            for (String service : statusMap.keySet()) {
                setStatusInternal(service, HealthCheckResponse.ServingStatus.NOT_SERVING);
            }
        }
    }

    private void notifyWatchers(String service, HealthCheckResponse.ServingStatus status) {
        HealthCheckResponse response = getResponseForWatch(status);
        IdentityHashMap<StreamObserver<HealthCheckResponse>, Boolean> serviceWatchers =
            watchers.get(service);
        if (serviceWatchers != null) {
            for (StreamObserver<HealthCheckResponse> responseObserver : serviceWatchers.keySet()) {
                responseObserver.onNext(response);
            }
        }
    }

    private static HealthCheckResponse getResponseForWatch(HealthCheckResponse.ServingStatus recordedStatus) {
        return HealthCheckResponse.newBuilder().setStatus(
            recordedStatus == null ? HealthCheckResponse.ServingStatus.SERVICE_UNKNOWN : recordedStatus).build();
    }
}
