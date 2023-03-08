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
import org.apache.dubbo.auth.v1alpha1.ObserveServiceGrpc;
import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.common.utils.IOUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.security.cert.rule.AuthorizationPolicy;

import io.grpc.Channel;
import io.grpc.Metadata;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.grpc.stub.StreamObserver;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static io.grpc.stub.MetadataUtils.newAttachHeadersInterceptor;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_SSL_CONNECT_INSECURE;

public class RuleManager {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(RuleManager.class);
    private final Map<String, String> latestRawRules = new ConcurrentHashMap<>();
    private final Map<String, List> latestRules = new ConcurrentHashMap<>();

    private final CertConfig certConfig;
    protected volatile Channel channel;
    private final FrameworkModel frameworkModel;

    private volatile StreamObserver<ObserveRequest> requestStreamObserver;

    public RuleManager(FrameworkModel frameworkModel, CertConfig certConfig) {
        this.frameworkModel = frameworkModel;
        this.certConfig = certConfig;
        connect();
    }

    private void connect() {
        connect0(certConfig);
        try {
            observe();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void connect0(CertConfig certConfig) {
        String caCertPath = certConfig.getCaCertPath();
        String remoteAddress = certConfig.getRemoteAddress();
        logger.info("Try to connect to Dubbo Cert Authority server: " + remoteAddress + ", caCertPath: " + remoteAddress);
        try {
            if (StringUtils.isNotEmpty(caCertPath)) {
                channel = NettyChannelBuilder.forTarget(remoteAddress)
                    .sslContext(
                        GrpcSslContexts.forClient()
                            .trustManager(new File(caCertPath))
                            .build())
                    .build();
            } else {
                logger.warn(CONFIG_SSL_CONNECT_INSECURE, "", "",
                    "No caCertPath is provided, will use insecure connection.");
                channel = NettyChannelBuilder.forTarget(remoteAddress)
                    .sslContext(GrpcSslContexts.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .build())
                    .build();
            }
        } catch (Exception e) {
            logger.error(LoggerCodeConstants.CONFIG_SSL_PATH_LOAD_FAILED, "", "", "Failed to load SSL cert file.", e);
            throw new RuntimeException(e);
        }
    }

    private void observe() throws IOException {
        ObserveServiceGrpc.ObserveServiceStub stub = ObserveServiceGrpc.newStub(channel);
        stub = setHeaderIfNeed(stub);
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

    private void recover() {
        if (requestStreamObserver != null) {
            requestStreamObserver.onCompleted();
        }
        requestStreamObserver = null;
        logger.info("Reconnect to Dubbo Cert Authority server: " + certConfig.getRemoteAddress());

        frameworkModel.getBeanFactory().getBean(FrameworkExecutorRepository.class)
            .getSharedScheduledExecutor()
            .schedule(()->{
                try {
                    connect();
                } catch (Throwable t) {
                    logger.info("Reconnect to Dubbo Cert Authority server failed: " + certConfig.getRemoteAddress());
                    recover();
                }
            }, certConfig.getRefreshInterval(), TimeUnit.MILLISECONDS);
    }

    public List getRules(String type) {
        return latestRules.get(type);
    }

    private ObserveServiceGrpc.ObserveServiceStub setHeaderIfNeed(ObserveServiceGrpc.ObserveServiceStub stub) throws IOException {
        String oidcTokenPath = certConfig.getOidcTokenPath();
        String oidcTokenType = certConfig.getOidcTokenType();
        if (StringUtils.isNotEmpty(oidcTokenPath)) {
            Metadata header = new Metadata();
            Metadata.Key<String> key = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
            header.put(key, "Bearer " +
                IOUtils.read(new FileReader(oidcTokenPath))
                    .replace("\n", "")
                    .replace("\t", "")
                    .replace("\r", "")
                    .trim());

            stub = stub.withInterceptors(newAttachHeadersInterceptor(header));
            logger.info("Use oidc token from " + oidcTokenPath + " to connect to Dubbo Certificate Authority.");
        } else {
            logger.warn(CONFIG_SSL_CONNECT_INSECURE, "", "",
                "Use insecure connection to connect to Dubbo Certificate Authority. Reason: No oidc token is provided.");
        }

        if (StringUtils.isNotEmpty(oidcTokenType)) {
            Metadata header = new Metadata();
            Metadata.Key<String> key = Metadata.Key.of("authorization-type", Metadata.ASCII_STRING_MARSHALLER);
            header.put(key, oidcTokenType);
            stub = stub.withInterceptors(newAttachHeadersInterceptor(header));
        }

        return stub;
    }


    private static class Handler implements StreamObserver<ObserveResponse> {
        private final RuleManager ruleManager;

        public Handler(RuleManager ruleManager) {
            this.ruleManager = ruleManager;
        }

        @Override
        public void onNext(ObserveResponse value) {
            ruleManager.latestRawRules.put(value.getType(), value.getData());
            if (value.getType().equals("authentication/v1beta1")) {
//                ruleManager.latestRules.put(value.getType(), AuthenticationPolicy.parse(value.getData()));
            } else if (value.getType().equals("authorization/v1beta1")) {
                ruleManager.latestRules.put(value.getType(), AuthorizationPolicy.parse(value.getData()));
            }
            ruleManager.requestStreamObserver.onNext(ObserveRequest.newBuilder()
                .setType(value.getType())
                .setNonce(value.getNonce())
                .build());
        }

        @Override
        public void onError(Throwable t) {
            ruleManager.recover();
        }

        @Override
        public void onCompleted() {
            ruleManager.recover();
        }
    }
}
