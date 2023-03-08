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

import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.deploy.ApplicationDeployListener;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.SslConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.Objects;
import java.util.Optional;

public class CertDeployerListener implements ApplicationDeployListener {
    private final DubboCertManager dubboCertManager;


    public CertDeployerListener(FrameworkModel frameworkModel) {
        dubboCertManager = frameworkModel.getBeanFactory().getBean(DubboCertManager.class);
    }

    @Override
    public void onStarting(ApplicationModel scopeModel) {
        Optional<SslConfig> config = scopeModel.getApplicationConfigManager().getSsl();
        if (config.isPresent()) {
            SslConfig sslConfig = config.get();
            if (Objects.nonNull(sslConfig.getCaAddress()) && dubboCertManager != null) {
                CertConfig certConfig = new CertConfig(sslConfig.getCaAddress(),
                    sslConfig.getEnvType(),
                    sslConfig.getCaCertPath(),
                    sslConfig.getOidcTokenPath(),
                    sslConfig.getOidcTokenType());
                dubboCertManager.connect(certConfig);

                if (dubboCertManager.generateCert() == null) {
                    System.exit(0);
                }
                return;
            }
        }
        String caAddress = ConfigurationUtils.getProperty(scopeModel, "DUBBO_CA_ADDRESS");
        String caCertPath = ConfigurationUtils.getProperty(scopeModel, "DUBBO_CA_CERT_PATH");
        String oidcToken = ConfigurationUtils.getProperty(scopeModel, "DUBBO_OIDC_TOKEN");
        String oidcTokenType = ConfigurationUtils.getProperty(scopeModel, "DUBBO_OIDC_TOKEN_TYPE");

        if (StringUtils.isNotEmpty(caAddress) &&
            StringUtils.isNotEmpty(caCertPath) &&
            StringUtils.isNotEmpty(oidcToken) &&
            StringUtils.isNotEmpty(oidcTokenType) &&
            dubboCertManager != null) {
            CertConfig certConfig = new CertConfig(caAddress,
                null,
                caCertPath,
                oidcToken,
                oidcTokenType);
            dubboCertManager.connect(certConfig);

            if (dubboCertManager.generateCert() == null) {
                System.exit(0);
            }
        }
    }

    @Override
    public void onStarted(ApplicationModel scopeModel) {
    }

    @Override
    public void onStopping(ApplicationModel scopeModel) {
        dubboCertManager.disConnect();
    }

    @Override
    public void onStopped(ApplicationModel scopeModel) {

    }

    @Override
    public void onFailure(ApplicationModel scopeModel, Throwable cause) {
        dubboCertManager.disConnect();
    }
}
