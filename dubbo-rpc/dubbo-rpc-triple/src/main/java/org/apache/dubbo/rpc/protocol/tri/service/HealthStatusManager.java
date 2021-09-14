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
import grpc.health.v1.HealthCheckResponse;

public class HealthStatusManager {

    /**
     * The special "service name" that represent all services on a GRPC server.  It is an empty string.
     */
    public static final String SERVICE_NAME_ALL_SERVICES = "";

    private final TriHealthImpl healthService;

    public HealthStatusManager(TriHealthImpl healthService) {
        this.healthService = healthService;
    }

    public Health getHealthService() {
        return healthService;
    }

    /**
     * Updates the status of the server.
     *
     * @param service the name of some aspect of the server that is associated with a health status. This name can have
     *                no relation with the gRPC services that the server is running with. It can also be an empty String
     *                {@code ""} per the gRPC specification.
     * @param status  is one of the values {@link HealthCheckResponse.ServingStatus#SERVING}, {@link
     *                HealthCheckResponse.ServingStatus#NOT_SERVING} and
     *                {@link HealthCheckResponse.ServingStatus#UNKNOWN}.
     */
    public void setStatus(String service, HealthCheckResponse.ServingStatus status) {
        healthService.setStatus(service, status);
    }

    /**
     * Clears the health status record of a service. The health service will respond with NOT_FOUND error on checking
     * the status of a cleared service.
     *
     * @param service the name of some aspect of the server that is associated with a health status. This name can have
     *                no relation with the gRPC services that the server is running with. It can also be an empty String
     *                {@code ""} per the gRPC specification.
     */
    public void clearStatus(String service) {
        healthService.clearStatus(service);
    }

    /**
     * enterTerminalState causes the health status manager to mark all services as not serving, and prevents future
     * updates to services.  This method is meant to be called prior to server shutdown as a way to indicate that
     * clients should redirect their traffic elsewhere.
     */
    public void enterTerminalState() {
        healthService.enterTerminalState();
    }
}
