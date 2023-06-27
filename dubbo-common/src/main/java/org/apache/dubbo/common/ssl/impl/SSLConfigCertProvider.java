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
package org.apache.dubbo.common.ssl.impl;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.ssl.AuthPolicy;
import org.apache.dubbo.common.ssl.Cert;
import org.apache.dubbo.common.ssl.CertProvider;
import org.apache.dubbo.common.ssl.ProviderCert;
import org.apache.dubbo.common.utils.IOUtils;

import java.io.IOException;
import java.util.Objects;

@Activate(order = Integer.MAX_VALUE - 10000)
public class SSLConfigCertProvider implements CertProvider {
    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(SSLConfigCertProvider.class);

    @Override
    public boolean isSupport(URL address) {
        return address.getOrDefaultApplicationModel().getApplicationConfigManager().getSsl()
            .isPresent();
    }

    @Override
    public ProviderCert getProviderConnectionConfig(URL localAddress) {
        return localAddress.getOrDefaultApplicationModel().getApplicationConfigManager().getSsl()
            .filter(sslConfig -> Objects.nonNull(sslConfig.getServerKeyCertChainPath()))
            .filter(sslConfig -> Objects.nonNull(sslConfig.getServerPrivateKeyPath()))
            .map(sslConfig -> {
                try {
                    return new ProviderCert(
                        IOUtils.toByteArray(sslConfig.getServerKeyCertChainPathStream()),
                        IOUtils.toByteArray(sslConfig.getServerPrivateKeyPathStream()),
                        sslConfig.getServerTrustCertCollectionPath() != null ? IOUtils.toByteArray(sslConfig.getServerTrustCertCollectionPathStream()) : null,
                        sslConfig.getServerKeyPassword(), AuthPolicy.CLIENT_AUTH);
                } catch (IOException e) {
                    logger.warn(LoggerCodeConstants.CONFIG_SSL_PATH_LOAD_FAILED, "", "", "Failed to load ssl config.", e);
                    return null;
                }
            }).orElse(null);
    }

    @Override
    public Cert getConsumerConnectionConfig(URL remoteAddress) {
        return remoteAddress.getOrDefaultApplicationModel().getApplicationConfigManager().getSsl()
            .filter(sslConfig -> Objects.nonNull(sslConfig.getClientKeyCertChainPath()))
            .filter(sslConfig -> Objects.nonNull(sslConfig.getClientPrivateKeyPath()))
            .map(sslConfig -> {
                try {
                    return new Cert(
                        IOUtils.toByteArray(sslConfig.getClientKeyCertChainPathStream()),
                        IOUtils.toByteArray(sslConfig.getClientPrivateKeyPathStream()),
                        sslConfig.getClientTrustCertCollectionPath() != null ? IOUtils.toByteArray(sslConfig.getClientTrustCertCollectionPathStream()) : null,
                        sslConfig.getClientKeyPassword());
                } catch (IOException e) {
                    logger.warn(LoggerCodeConstants.CONFIG_SSL_PATH_LOAD_FAILED, "", "", "Failed to load ssl config.", e);
                    return null;
                }
            }).orElse(null);
    }
}
