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
package org.apache.dubbo.security.cert;

import org.apache.dubbo.auth.v1alpha1.ObserveRequest;
import org.apache.dubbo.auth.v1alpha1.ObserveResponse;
import org.apache.dubbo.auth.v1alpha1.RuleServiceGrpc;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.security.cert.rule.authentication.AuthenticationPolicy;
import org.apache.dubbo.security.cert.rule.authorization.AuthorizationPolicy;

import io.grpc.Channel;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AuthorityRuleSync {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(AuthorityRuleSync.class);
    private final Map<String, String> latestRawRules = new ConcurrentHashMap<>();
    private volatile List<AuthorizationPolicy> latestAuthorizationPolicies = null;
    private volatile List<AuthenticationPolicy> latestAuthenticationPolicies = null;

    private final AuthorityConnector connector;
    private final CertConfig certConfig;
    private final FrameworkModel frameworkModel;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    private volatile StreamObserver<ObserveRequest> requestStreamObserver;

    public AuthorityRuleSync(FrameworkModel frameworkModel, CertConfig certConfig, AuthorityConnector connector) {
        this.frameworkModel = frameworkModel;
        this.certConfig = certConfig;
        this.connector = connector;
        observe();
    }

    private synchronized void observe() {
        if (shutdown.get()) {
            return;
        }
        try {
            observe0();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected synchronized void disConnect() {
        shutdown.set(true);
        if (requestStreamObserver != null) {
            requestStreamObserver.onCompleted();
            requestStreamObserver = null;
        }
    }

    private void observe0() throws IOException {
        if (!connector.isConnected()) {
            recover();
            return;
        }
        Channel channel = connector.generateChannel();
        if (channel == null) {
            recover();
            return;
        }
        RuleServiceGrpc.RuleServiceStub stub = RuleServiceGrpc.newStub(channel);
        stub = connector.setHeaders(stub);
        requestStreamObserver = stub.observe(new Handler(this));
        requestStreamObserver.onNext(ObserveRequest.newBuilder()
            .setNonce("")
            .setType("authentication/v1beta1")
            .build());
        requestStreamObserver.onNext(ObserveRequest.newBuilder()
            .setNonce("")
            .setType("authorization/v1beta1")
            .build());
    }

    private synchronized void recover() {
        if (shutdown.get()) {
            return;
        }
        if (requestStreamObserver != null) {
            requestStreamObserver.onCompleted();
        }
        requestStreamObserver = null;
        logger.info("Reconnect to Dubbo Cert Authority server: " + certConfig.getRemoteAddress());

        frameworkModel.getBeanFactory().getBean(FrameworkExecutorRepository.class)
            .getSharedScheduledExecutor()
            .schedule(() -> {
                try {
                    observe();
                } catch (Throwable t) {
                    logger.info("Reconnect to Dubbo Cert Authority server failed: " + certConfig.getRemoteAddress());
                    recover();
                }
            }, certConfig.getRefreshInterval(), TimeUnit.MILLISECONDS);
    }

    public List<AuthorizationPolicy> getLatestAuthorizationPolicies() {
        return latestAuthorizationPolicies;
    }

    public List<AuthenticationPolicy> getLatestAuthenticationPolicies() {
        return latestAuthenticationPolicies;
    }

    private static class Handler implements StreamObserver<ObserveResponse> {
        private final AuthorityRuleSync authorityRuleSync;

        public Handler(AuthorityRuleSync authorityRuleSync) {
            this.authorityRuleSync = authorityRuleSync;
        }

        @Override
        public void onNext(ObserveResponse value) {
            authorityRuleSync.latestRawRules.put(value.getType(), value.getData());
            if (value.getType().equals("authentication/v1beta1")) {
                authorityRuleSync.latestAuthenticationPolicies = AuthenticationPolicy.parse(value.getData());
            } else if (value.getType().equals("authorization/v1beta1")) {
                authorityRuleSync.latestAuthorizationPolicies = AuthorizationPolicy.parse(value.getData());
            }
            authorityRuleSync.requestStreamObserver.onNext(ObserveRequest.newBuilder()
                .setType(value.getType())
                .setNonce(value.getNonce())
                .build());
        }

        @Override
        public void onError(Throwable t) {
            authorityRuleSync.recover();
        }

        @Override
        public void onCompleted() {
            authorityRuleSync.recover();
        }
    }
}
