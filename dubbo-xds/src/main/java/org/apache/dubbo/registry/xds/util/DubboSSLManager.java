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
package org.apache.dubbo.registry.xds.util;

import org.apache.dubbo.config.SslConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.registry.xds.XdsCertificateSigner;
import org.apache.dubbo.rpc.model.ApplicationModel;

public class DubboSSLManager {

    public static void setCert(ApplicationModel applicationModel, XdsCertificateSigner.CertPair certPair) {
        ConfigManager configManager = applicationModel.getApplicationConfigManager();

        SslConfig sslConfig = new SslConfig();

        sslConfig.setClientKeyCertChain(certPair.getCertChain());
        sslConfig.setClientPrivateKey(certPair.getPrivateKey());
        sslConfig.setClientTrustCertCollection(certPair.getPrivateKey());

        sslConfig.setServerKeyCertChain(certPair.getCertChain());
        sslConfig.setServerPrivateKey(certPair.getPrivateKey());
        sslConfig.setServerTrustCertCollection(certPair.getPrivateKey());

        configManager.replaceSsl(sslConfig);
    }
}
