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
package org.apache.dubbo.xds.security.api;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.ssl.AuthPolicy;
import org.apache.dubbo.common.ssl.Cert;
import org.apache.dubbo.common.ssl.CertProvider;
import org.apache.dubbo.common.ssl.ProviderCert;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.xds.PilotExchanger;
import org.apache.dubbo.xds.istio.IstioEnv;
import org.apache.dubbo.xds.listener.XdsTlsConfigRepository;
import org.apache.dubbo.xds.security.authn.DownstreamTlsConfig;
import org.apache.dubbo.xds.security.authn.SecretConfig;
import org.apache.dubbo.xds.security.authn.UpstreamTlsConfig;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER;

@Activate
public class XdsCertProvider implements CertProvider {

    private final List<TrustSource> trustSource;

    private final List<CertSource> certSource;

    private final XdsTlsConfigRepository configRepo;

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(XdsCertProvider.class);

    private final IstioEnv istioEnv = IstioEnv.getInstance();

    public XdsCertProvider(FrameworkModel frameworkModel) {
        this.configRepo = frameworkModel.getBeanFactory().getOrRegisterBean(XdsTlsConfigRepository.class);
        if (frameworkModel.getBeanFactory().getBean(PilotExchanger.class) == null) {
            logger.info("XdsCertProvider won't initialize because XDS Client not found.");
            this.trustSource = Collections.emptyList();
            this.certSource = Collections.emptyList();
            return;
        }
        this.trustSource = frameworkModel.getExtensionLoader(TrustSource.class).getActivateExtensions();
        this.certSource = frameworkModel.getExtensionLoader(CertSource.class).getActivateExtensions();
    }

    @Override
    public boolean isSupport(URL address) {
        String side = address.getSide();
        if (CONSUMER.equals(side)) {
            // TODO: If XDS URL can support version tag, key should be address.getServiceKey()
            UpstreamTlsConfig upstreamConfig = configRepo.getUpstreamConfig(address.getServiceInterface());
            if (upstreamConfig == null || upstreamConfig.getGeneralTlsConfig() == null) {
                return false;
            }
            List<SecretConfig> trustConfigs =
                    upstreamConfig.getGeneralTlsConfig().trustConfigs();
            List<SecretConfig> certConfigs =
                    upstreamConfig.getGeneralTlsConfig().certConfigs();

            // At least one config provided by LDS
            return !trustConfigs.isEmpty() || !certConfigs.isEmpty();
        } else if (PROVIDER.equals(side)) {
            DownstreamTlsConfig downstreamConfig = configRepo.getDownstreamConfig(String.valueOf(address.getPort()));
            if (downstreamConfig == null) {
                return false;
            }
            List<SecretConfig> secretConfigs =
                    downstreamConfig.getGeneralTlsConfig().certConfigs();
            List<SecretConfig> certConfigs =
                    downstreamConfig.getGeneralTlsConfig().trustConfigs();

            // At least one config provided by CDS
            return !secretConfigs.isEmpty() || !certConfigs.isEmpty();
        }
        throw new IllegalStateException("Can't determine side for url:" + address);

        // seems we don't need url to check here anymore
        //        if (TlsType.PERMISSIVE.equals(type)) {
        //            String security = address.getParameter("security");
        //            String mesh = address.getParameter("mesh");
        //            return mesh != null
        //                    && security != null
        //                    && Arrays.asList(security.split(",")).contains("mTLS");
        //        }
    }

    @Override
    public ProviderCert getProviderConnectionConfig(URL localAddress) {
        DownstreamTlsConfig downstreamConfig = configRepo.getDownstreamConfig(String.valueOf(localAddress.getPort()));

        if (downstreamConfig == null || downstreamConfig.getGeneralTlsConfig() == null) {
            logger.warn("99-0", "", "", "DownstreamTlsConfig is null for localAddress:" + localAddress);
            return null;
        }

        CertPair cert = selectCertConfig(
                localAddress, downstreamConfig.getGeneralTlsConfig().certConfigs());
        X509CertChains trust = selectTrustConfig(
                localAddress, downstreamConfig.getGeneralTlsConfig().trustConfigs());

        AuthPolicy authPolicy;
        switch (downstreamConfig.getTlsType()) {
            case STRICT:
                authPolicy = AuthPolicy.CLIENT_AUTH_STRICT;
                break;
            case PERMISSIVE:
                authPolicy = AuthPolicy.CLIENT_AUTH_PERMISSIVE;
                break;
            case DISABLE:
                authPolicy = AuthPolicy.NONE;
                break;
            default:
                throw new IllegalStateException("Unexpected Tls type: " + downstreamConfig.getTlsType());
        }
        return new ProviderCert(
                cert == null ? null : cert.getPublicKey().getBytes(StandardCharsets.UTF_8),
                cert == null ? null : cert.getPrivateKey().getBytes(StandardCharsets.UTF_8),
                trust == null ? null : trust.readAsBytes(),
                cert == null ? null : cert.getPassword(),
                authPolicy);
    }

    @Override
    public Cert getConsumerConnectionConfig(URL remoteAddress) {
        UpstreamTlsConfig downstreamConfig = configRepo.getUpstreamConfig(remoteAddress.getServiceInterface());

        if (downstreamConfig == null) {
            logger.warn("99-0", "", "", "DownstreamTlsConfig is null for remoteUrl:" + remoteAddress);
            return null;
        }

        CertPair cert = selectCertConfig(
                remoteAddress, downstreamConfig.getGeneralTlsConfig().certConfigs());
        X509CertChains trust = selectTrustConfig(
                remoteAddress, downstreamConfig.getGeneralTlsConfig().trustConfigs());

        return new ProviderCert(
                cert == null ? null : cert.getPublicKey().getBytes(StandardCharsets.UTF_8),
                cert == null ? null : cert.getPrivateKey().getBytes(StandardCharsets.UTF_8),
                trust == null ? null : trust.readAsBytes(),
                cert == null ? null : cert.getPassword(),
                AuthPolicy.SERVER_AUTH);
    }

    private CertPair selectCertConfig(URL address, List<SecretConfig> certConfigs) {
        for (CertSource certSource : this.certSource) {
            SecretConfig secretConfig = certSource.selectSupportedCertConfig(address, certConfigs);
            if (secretConfig != null) {
                return certSource.getCert(address, secretConfig);
            }
        }
        return null;
    }

    private X509CertChains selectTrustConfig(URL address, List<SecretConfig> certConfigs) {
        for (TrustSource trustSource : this.trustSource) {
            SecretConfig secretConfig = trustSource.selectSupportedTrustConfig(address, certConfigs);
            if (secretConfig != null) {
                return trustSource.getTrustCerts(address, secretConfig);
            }
        }
        return null;
    }
}
