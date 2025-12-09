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
package org.apache.dubbo.xds.security.authn;

import org.apache.dubbo.xds.security.authn.FileSecretConfig.DefaultNames;
import org.apache.dubbo.xds.security.authn.SecretConfig.ConfigType;

import java.util.ArrayList;
import java.util.List;

import io.envoyproxy.envoy.config.core.v3.DataSource;
import io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.CertificateValidationContext;
import io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.CommonTlsContext;
import io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.CommonTlsContext.CombinedCertificateValidationContext;

public class TlsResourceResolver {

    public static GeneralTlsConfig resolveCommonTlsConfig(String configName, CommonTlsContext commonTlsContext) {
        List<SecretConfig> trustConfigs = new ArrayList<>();
        List<SecretConfig> certConfigs = new ArrayList<>();

        // sds cert sources
        List<io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.SdsSecretConfig> sdsCertConfigs =
                commonTlsContext.getTlsCertificateSdsSecretConfigsList();
        sdsCertConfigs.forEach(sdsSecretConfig -> certConfigs.add(new SdsSecretConfig(
                sdsSecretConfig.getName(),
                ConfigType.CERT,
                sdsSecretConfig.getSdsConfig().getApiConfigSource())));

        // file cert sources
        commonTlsContext.getTlsCertificatesList().forEach(tlsCertificate -> {
            DataSource certChain = tlsCertificate.getCertificateChain();
            DataSource privateKey = tlsCertificate.getPrivateKey();
            certConfigs.add(new FileSecretConfig(
                    DefaultNames.LOCAL_CERT.name(),
                    privateKey,
                    certChain,
                    tlsCertificate.hasPassword() ? tlsCertificate.getPassword() : null));
        });

        // sds trust sources
        io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.SdsSecretConfig sdsTrustConfig =
                commonTlsContext.getValidationContextSdsSecretConfig();
        if (commonTlsContext.hasValidationContextSdsSecretConfig()) {
            trustConfigs.add(new SdsSecretConfig(
                    sdsTrustConfig.getName(),
                    ConfigType.TRUST,
                    sdsTrustConfig.getSdsConfig().getApiConfigSource()));
        }

        // file trust sources
        if (commonTlsContext.hasValidationContext()
                && commonTlsContext.getValidationContext().hasTrustedCa()) {
            trustConfigs.add(new FileSecretConfig(
                    DefaultNames.LOCAL_TRUST.name(),
                    commonTlsContext.getValidationContext().getTrustedCa()));
        }

        CombinedCertificateValidationContext combinedConfig = commonTlsContext.getCombinedValidationContext();
        if (commonTlsContext.hasCombinedValidationContext()) {
            if (combinedConfig.hasValidationContextSdsSecretConfig()) {
                io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.SdsSecretConfig sdsConfig =
                        combinedConfig.getValidationContextSdsSecretConfig();
                trustConfigs.add(new SdsSecretConfig(
                        sdsConfig.getName(),
                        ConfigType.TRUST,
                        sdsConfig.getSdsConfig().getApiConfigSource()));
            }
            if (combinedConfig.hasDefaultValidationContext()) {
                CertificateValidationContext defaultConfig = combinedConfig.getDefaultValidationContext();
                if (defaultConfig.hasTrustedCa()) {
                    trustConfigs.add(
                            new FileSecretConfig(DefaultNames.LOCAL_TRUST.name(), defaultConfig.getTrustedCa()));
                }
            }
        }
        return new GeneralTlsConfig(configName, trustConfigs, certConfigs, commonTlsContext.getAlpnProtocolsList());
    }
}
