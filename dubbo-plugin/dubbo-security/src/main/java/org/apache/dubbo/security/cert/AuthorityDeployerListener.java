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

import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.deploy.ApplicationDeployListener;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.SslConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.Objects;
import java.util.Optional;

public class AuthorityDeployerListener implements ApplicationDeployListener {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(AuthorityDeployerListener.class);

    private static final boolean IS_SUPPORTED = isSupported();

    public static boolean isSupported() {
        try {
            ClassUtils.forName("io.grpc.Channel");
            ClassUtils.forName("org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder");
            logger.info("Found dubbo-security dependencies.");
            return true;
        } catch (Throwable t) {
            logger.info("Unable to find dubbo-security dependencies. Will disable dubbo authority.");
            return false;
        }
    }

    @Override
    public void onInitialize(ApplicationModel scopeModel) {

    }

    @Override
    public void onStarting(ApplicationModel scopeModel) {
        FrameworkModel frameworkModel = scopeModel.getFrameworkModel();
        Optional<SslConfig> config = scopeModel.getApplicationConfigManager().getSsl();
        if (config.isPresent()) {
            SslConfig sslConfig = config.get();
            if (Objects.nonNull(sslConfig.getCaAddress()) && IS_SUPPORTED) {
                CertConfig certConfig = new CertConfig(sslConfig.getCaAddress(),
                    sslConfig.getEnvType(),
                    sslConfig.getCaCertPath(),
                    sslConfig.getOidcTokenPath(),
                    sslConfig.getOidcTokenType());

                connect(frameworkModel, certConfig);
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
            IS_SUPPORTED) {
            CertConfig certConfig = new CertConfig(caAddress,
                null,
                caCertPath,
                oidcToken,
                oidcTokenType);

            connect(frameworkModel, certConfig);
        }
    }

    private static void connect(FrameworkModel frameworkModel, CertConfig certConfig) {
        ScopeBeanFactory beanFactory = frameworkModel.getBeanFactory();
        if (beanFactory.getBean(AuthorityConnector.class) != null) {
            return;
        }

        AuthorityConnector connector = new AuthorityConnector(frameworkModel, certConfig);
        beanFactory.registerBean(connector);
        frameworkModel.addDestroyListener(scope -> connector.disConnect());
    }

    @Override
    public void onStarted(ApplicationModel scopeModel) {
    }

    @Override
    public void onStopping(ApplicationModel scopeModel) {
        FrameworkModel frameworkModel = scopeModel.getFrameworkModel();
        AuthorityConnector connector = frameworkModel.getBeanFactory().getBean(AuthorityConnector.class);
        if (Objects.nonNull(connector)) {
            connector.disConnect();
        }
    }

    @Override
    public void onStopped(ApplicationModel scopeModel) {

    }

    @Override
    public void onFailure(ApplicationModel scopeModel, Throwable cause) {

    }
}
