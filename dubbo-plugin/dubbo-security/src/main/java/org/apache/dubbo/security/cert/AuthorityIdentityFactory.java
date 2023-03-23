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

import org.apache.dubbo.auth.v1alpha1.AuthorityServiceGrpc;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_SSL_CERT_GENERATE_FAILED;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_FAILED_GENERATE_CERT_ISTIO;

public class AuthorityIdentityFactory {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(AuthorityIdentityFactory.class);

    private final FrameworkModel frameworkModel;
    private final AuthorityConnector connector;
    /**
     * Cert pair for current Dubbo instance
     */
    protected volatile CertConfig certConfig;
    /**
     * Refresh cert pair for current Dubbo instance
     */
    protected volatile ScheduledFuture<?> refreshFuture;
    protected volatile IdentityInfo identityInfo;

    public AuthorityIdentityFactory(FrameworkModel frameworkModel, CertConfig certConfig, AuthorityConnector connector) {
        this.frameworkModel = frameworkModel;
        this.certConfig = certConfig;
        this.connector = connector;
        connect();
    }

    private void connect() {
        // Try to generate cert from remote
        generateIdentity();
        // Schedule refresh task
        scheduleRefresh();
    }

    /**
     * Create task to refresh cert pair for current Dubbo instance
     */
    protected void scheduleRefresh() {
        FrameworkExecutorRepository repository = frameworkModel.getBeanFactory().getBean(FrameworkExecutorRepository.class);
        refreshFuture = repository.getSharedScheduledExecutor().scheduleAtFixedRate(this::generateIdentity,
            certConfig.getRefreshInterval(), certConfig.getRefreshInterval(), TimeUnit.MILLISECONDS);
    }

    public synchronized void disConnect() {
        if (refreshFuture != null) {
            refreshFuture.cancel(true);
            refreshFuture = null;
        }
    }

    public boolean isConnected() {
        return certConfig != null && identityInfo != null;
    }

    protected IdentityInfo generateIdentity() {
        if (identityInfo != null && !identityInfo.isExpire()) {
            return identityInfo;
        }
        synchronized (this) {
            if (identityInfo == null || identityInfo.isExpire()) {
                try {
                    logger.info("Try to generate cert from Dubbo Certificate Authority.");
                    AuthorityServiceGrpc.AuthorityServiceBlockingStub stub = AuthorityServiceGrpc.newBlockingStub(connector.generateChannel());
                    stub = connector.setHeaders(stub);
                    IdentityInfo certFromRemote = CertServiceUtil.refreshCert(stub, "CONNECTION");
                    if (certFromRemote != null) {
                        identityInfo = certFromRemote;
                    } else {
                        logger.error(CONFIG_SSL_CERT_GENERATE_FAILED, "", "", "Generate Cert from Dubbo Certificate Authority failed.");
                    }
                } catch (Exception e) {
                    logger.error(REGISTRY_FAILED_GENERATE_CERT_ISTIO, "", "", "Generate Cert from Istio failed.", e);
                }
            }
        }
        return identityInfo;
    }
}
