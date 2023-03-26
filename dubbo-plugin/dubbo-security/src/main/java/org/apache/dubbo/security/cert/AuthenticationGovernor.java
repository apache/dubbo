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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.ssl.AuthPolicy;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.config.ServiceConfigBase;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryFactory;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.security.cert.rule.authentication.AuthenticationAction;
import org.apache.dubbo.security.cert.rule.authentication.AuthenticationPolicy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_SERVER_SHUTDOWN_TIMEOUT;
import static org.apache.dubbo.common.constants.CommonConstants.SSL_ENABLED_KEY;

public class AuthenticationGovernor {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(AuthenticationGovernor.class);
    private final FrameworkModel frameworkModel;

    private volatile AuthorityRuleSync authorityRuleSync;
    private final AtomicBoolean inUpdating = new AtomicBoolean(false);

    public AuthenticationGovernor(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    public AuthPolicy getPortPolicy(int port) {
        obtainRuleSync();
        if (authorityRuleSync == null) {
            return AuthPolicy.NONE;
        }

        List<AuthenticationPolicy> authenticationPolicies = authorityRuleSync.getLatestAuthenticationPolicies();
        if (authenticationPolicies == null || authenticationPolicies.isEmpty()) {
            return AuthPolicy.NONE;
        }

        for (AuthenticationPolicy authenticationPolicy : authenticationPolicies) {
            AuthenticationAction action = authenticationPolicy.match(port);
            if (action != null) {
                return action.toAuthPolicy();
            }
        }
        return AuthPolicy.NONE;
    }

    private void obtainRuleSync() {
        if (authorityRuleSync == null) {
            authorityRuleSync = frameworkModel.getBeanFactory().getBean(AuthorityRuleSync.class);

            if (authorityRuleSync != null) {
                synchronized (this) {
                    frameworkModel.getBeanFactory().getBean(FrameworkExecutorRepository.class)
                        .getSharedScheduledExecutor().scheduleAtFixedRate(this::checkRuleUpdate,
                            1, 1, TimeUnit.SECONDS);
                }
            }
        }
    }

    protected void checkRuleUpdate() {
        frameworkModel.getBeanFactory().getBean(FrameworkExecutorRepository.class)
            .getSharedExecutor().submit(this::checkRuleUpdate0);
    }

    protected void checkRuleUpdate0() {
        if (!inUpdating.compareAndSet(false, true)) {
            return;
        }
        try {
            obtainRuleSync();

            if (authorityRuleSync == null) {
                return;
            }

            Set<ProviderModel> providersToReexport = new HashSet<>();

            List<ProviderModel> providerModels = frameworkModel.getServiceRepository().allProviderModels();
            for (ProviderModel providerModel : providerModels) {
                for (ProviderModel.RegisterStatedURL registerStatedURL : providerModel.getStatedUrl()) {
                    if (!checkProviderUrl(registerStatedURL.getProviderUrl())) {
                        providersToReexport.add(providerModel);
                    }
                }
            }

            int waitTime = DEFAULT_SERVER_SHUTDOWN_TIMEOUT;
            for (ProviderModel providerModel : providersToReexport) {
                for (ProviderModel.RegisterStatedURL registerStatedURL : providerModel.getStatedUrl()) {
                    if (registerStatedURL.isRegistered()) {
                        doUnExport(registerStatedURL);
                        waitTime = Math.max(waitTime, ConfigurationUtils.getServerShutdownTimeout(providerModel.getModuleModel()));
                        logger.info("Unregister provider url: " + registerStatedURL.getProviderUrl() + " for ssl status changed purpose.");
                    }
                }
            }

            if (providersToReexport.isEmpty()) {
                return;
            }

            logger.info("Wait for " + waitTime + "ms to re-export providers for ssl status changed purpose.");
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            List<ProviderModel> metadataServices = providersToReexport.stream()
                .filter(providerModel -> MetadataService.class.equals(providerModel.getServiceInterfaceClass()))
                .collect(Collectors.toList());

            if (!metadataServices.isEmpty()) {
                metadataServices.forEach(providersToReexport::remove);

                for (ProviderModel metadataService : metadataServices) {
                    ServiceConfigBase<?> serviceConfig = metadataService.getServiceConfig();
                    serviceConfig.unexport();
                    serviceConfig.export();
                    logger.info("Re-export provider: " + metadataService.getServiceName() + " for ssl status changed purpose.");
                }
            }

            for (ProviderModel providerModel : providersToReexport) {
                providerModel.getServiceConfig().unexport();
                logger.info("Un-export provider: " + providerModel.getServiceName() + " for ssl status changed purpose.");
            }

            for (ProviderModel providerModel : providersToReexport) {
                providerModel.getServiceConfig().export();
                logger.info("Re-export provider: " + providerModel.getServiceName() + " for ssl status changed purpose.");
            }
        } finally {
            inUpdating.set(false);
        }
    }

    private boolean checkProviderUrl(URL providerUrl) {
        AuthPolicy portPolicy = getPortPolicy(providerUrl.getPort());
        if (portPolicy == null || portPolicy == AuthPolicy.NONE) {
            return !providerUrl.getParameter(SSL_ENABLED_KEY, false);
        } else {
            return providerUrl.getParameter(SSL_ENABLED_KEY, false);
        }
    }

    protected void doUnExport(ProviderModel.RegisterStatedURL statedURL) {
        RegistryFactory registryFactory =
            statedURL.getRegistryUrl().getOrDefaultApplicationModel().getExtensionLoader(RegistryFactory.class).getAdaptiveExtension();
        Registry registry = registryFactory.getRegistry(statedURL.getRegistryUrl());
        registry.unregister(statedURL.getProviderUrl());
        statedURL.setRegistered(false);
    }
}
