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

import org.apache.dubbo.rpc.StatusRpcException;
import org.apache.dubbo.rpc.TriRpcStatus.Code;

import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

class HealthStatusManagerTest {

    private final TriHealthImpl health = new TriHealthImpl();
    private final HealthStatusManager manager = new HealthStatusManager(health);

    @Test
    void getHealthService() {
        Assertions.assertNotNull(manager.getHealthService());
    }

    @Test
    void setStatus() {
        String service = "serv0";
        manager.setStatus(service, ServingStatus.SERVING);
        ServingStatus stored = manager.getHealthService().check(HealthCheckRequest.newBuilder()
            .setService(service)
            .build()).getStatus();
        Assertions.assertEquals(ServingStatus.SERVING, stored);
    }

    @Test
    void clearStatus() {
        String service = "serv1";
        manager.setStatus(service, ServingStatus.SERVING);
        ServingStatus stored = manager.getHealthService().check(HealthCheckRequest.newBuilder()
            .setService(service)
            .build()).getStatus();
        Assertions.assertEquals(ServingStatus.SERVING, stored);
        manager.clearStatus(service);
        try {
            manager.getHealthService().check(HealthCheckRequest.newBuilder()
                .setService(service)
                .build());
            fail();
        } catch (StatusRpcException e) {
            Assertions.assertEquals(Code.NOT_FOUND, e.getStatus().code);
        }
    }

    @Test
    void enterTerminalState() {
        String service = "serv2";
        manager.setStatus(service, ServingStatus.SERVING);
        ServingStatus stored = manager.getHealthService().check(HealthCheckRequest.newBuilder()
            .setService(service)
            .build()).getStatus();
        Assertions.assertEquals(ServingStatus.SERVING, stored);
        manager.enterTerminalState();
        ServingStatus stored2 = manager.getHealthService().check(HealthCheckRequest.newBuilder()
            .setService(service)
            .build()).getStatus();
        Assertions.assertEquals(ServingStatus.NOT_SERVING, stored2);
    }
}
