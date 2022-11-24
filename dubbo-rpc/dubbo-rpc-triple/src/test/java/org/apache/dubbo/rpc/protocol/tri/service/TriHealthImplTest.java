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

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;

import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.IdentityHashMap;

/**
 * {@link TriHealthImpl}
 */
class TriHealthImplTest {

    @Test
    void testCheck() {
        TriHealthImpl triHealth = new TriHealthImpl();

        HealthCheckRequest request = HealthCheckRequest.newBuilder().build();
        HealthCheckResponse response = triHealth.check(request);
        Assertions.assertEquals(response.getStatus(), HealthCheckResponse.ServingStatus.SERVING);

        HealthCheckRequest badRequest = HealthCheckRequest.newBuilder().setService("test").build();
        Assertions.assertThrows(RpcException.class, () -> triHealth.check(badRequest));
    }

    @Test
    void testWatch() throws Exception {
        TriHealthImpl triHealth = new TriHealthImpl();

        HealthCheckRequest request = HealthCheckRequest.newBuilder()
            .setService("testWatch")
            .build();
        triHealth.setStatus(request.getService(), ServingStatus.SERVING);
        StreamObserver<HealthCheckResponse> streamObserver = new MockStreamObserver();

        RpcContext.removeCancellationContext();
        // test watch
        triHealth.watch(request, streamObserver);
        Assertions.assertNotNull(RpcContext.getCancellationContext().getListeners());
        HashMap<String, IdentityHashMap<StreamObserver<HealthCheckResponse>, Boolean>> watches = getWatches(
            triHealth);
        Assertions.assertTrue(watches.containsKey(request.getService()));
        Assertions.assertTrue(watches.get(request.getService()).containsKey(streamObserver));
        Assertions.assertTrue(watches.get(request.getService()).get(streamObserver));
        MockStreamObserver mockStreamObserver = (MockStreamObserver) streamObserver;
        Assertions.assertEquals(mockStreamObserver.getCount(), 1);
        Assertions.assertEquals(mockStreamObserver.getResponse().getStatus(),
            HealthCheckResponse.ServingStatus.SERVING);

        // test setStatus
        triHealth.setStatus(request.getService(),
            HealthCheckResponse.ServingStatus.SERVICE_UNKNOWN);
        Assertions.assertEquals(mockStreamObserver.getCount(), 2);
        Assertions.assertEquals(mockStreamObserver.getResponse().getStatus(),
            HealthCheckResponse.ServingStatus.SERVICE_UNKNOWN);

        triHealth.enterTerminalState();
        Assertions.assertEquals(mockStreamObserver.getCount(), 3);
        Assertions.assertEquals(mockStreamObserver.getResponse().getStatus(),
            HealthCheckResponse.ServingStatus.NOT_SERVING);

        // test clearStatus
        turnOffTerminal(triHealth);
        triHealth.clearStatus(request.getService());
        Assertions.assertEquals(mockStreamObserver.getCount(), 4);
        Assertions.assertEquals(mockStreamObserver.getResponse().getStatus(),
            HealthCheckResponse.ServingStatus.SERVICE_UNKNOWN);

        // test listener
        RpcContext.getCancellationContext().close();
        Assertions.assertTrue(watches.isEmpty());
    }

    private void turnOffTerminal(TriHealthImpl triHealth)
        throws NoSuchFieldException, IllegalAccessException {
        Field terminalField = triHealth.getClass().getDeclaredField("terminal");
        terminalField.setAccessible(true);
        terminalField.set(triHealth, false);
    }

    private HashMap<String, IdentityHashMap<StreamObserver<HealthCheckResponse>, Boolean>> getWatches(
        TriHealthImpl triHealth) throws Exception {
        Field watchersField = triHealth.getClass().getDeclaredField("watchers");
        watchersField.setAccessible(true);
        HashMap<String, IdentityHashMap<StreamObserver<HealthCheckResponse>, Boolean>> watches =
            (HashMap<String, IdentityHashMap<StreamObserver<HealthCheckResponse>, Boolean>>) watchersField.get(
                triHealth);
        return watches;
    }

    class MockStreamObserver implements StreamObserver<HealthCheckResponse> {

        private int count = 0;
        private HealthCheckResponse response;

        @Override
        public void onNext(HealthCheckResponse data) {
            count++;
            response = data;
        }

        @Override
        public void onError(Throwable throwable) {

        }

        @Override
        public void onCompleted() {

        }

        public int getCount() {
            return count;
        }

        public HealthCheckResponse getResponse() {
            return response;
        }
    }
}
