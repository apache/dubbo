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
package org.apache.dubbo.security.cert.impl;

import org.apache.dubbo.auth.v1alpha1.AuthorityServiceGrpc;
import org.apache.dubbo.auth.v1alpha1.IdentityRequest;
import org.apache.dubbo.auth.v1alpha1.IdentityResponse;

import io.grpc.stub.StreamObserver;

import java.util.concurrent.atomic.AtomicReference;

public class AuthorityServiceImpl extends AuthorityServiceGrpc.AuthorityServiceImplBase {
    private final AtomicReference<IdentityResponse> responseRef = new AtomicReference<>();
    private final AtomicReference<IdentityRequest> requestRef = new AtomicReference<>();

    @Override
    public void createIdentity(IdentityRequest request, StreamObserver<IdentityResponse> responseObserver) {
        requestRef.set(request);

        responseObserver.onNext(responseRef.get());
        responseObserver.onCompleted();
    }

    public AtomicReference<IdentityResponse> getResponseRef() {
        return responseRef;
    }

    public AtomicReference<IdentityRequest> getRequestRef() {
        return requestRef;
    }
}
