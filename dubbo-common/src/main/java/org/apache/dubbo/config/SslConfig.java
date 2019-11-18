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
package org.apache.dubbo.config;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

public class SslConfig extends AbstractConfig {

    private static final Logger logger = LoggerFactory.getLogger(SslConfig.class);

    private String serverKeyCertChainPath;
    private String serverPrivateKeyPath;
    private String serverKeyPassword;
    private String serverTrustCertCollectionPath;

    private String clientKeyCertChainPath;
    private String clientPrivateKeyPath;
    private String clientKeyPassword;
    private String clientTrustCertCollectionPath;

    public String getServerKeyCertChainPath() {
        return serverKeyCertChainPath;
    }

    public void setServerKeyCertChainPath(String serverKeyCertChainPath) {
        this.serverKeyCertChainPath = serverKeyCertChainPath;
    }

    public String getServerPrivateKeyPath() {
        return serverPrivateKeyPath;
    }

    public void setServerPrivateKeyPath(String serverPrivateKeyPath) {
        this.serverPrivateKeyPath = serverPrivateKeyPath;
    }

    public String getServerKeyPassword() {
        return serverKeyPassword;
    }

    public void setServerKeyPassword(String serverKeyPassword) {
        this.serverKeyPassword = serverKeyPassword;
    }

    public String getServerTrustCertCollectionPath() {
        return serverTrustCertCollectionPath;
    }

    public void setServerTrustCertCollectionPath(String serverTrustCertCollectionPath) {
        this.serverTrustCertCollectionPath = serverTrustCertCollectionPath;
    }

    public String getClientKeyCertChainPath() {
        return clientKeyCertChainPath;
    }

    public void setClientKeyCertChainPath(String clientKeyCertChainPath) {
        this.clientKeyCertChainPath = clientKeyCertChainPath;
    }

    public String getClientPrivateKeyPath() {
        return clientPrivateKeyPath;
    }

    public void setClientPrivateKeyPath(String clientPrivateKeyPath) {
        this.clientPrivateKeyPath = clientPrivateKeyPath;
    }

    public String getClientKeyPassword() {
        return clientKeyPassword;
    }

    public void setClientKeyPassword(String clientKeyPassword) {
        this.clientKeyPassword = clientKeyPassword;
    }

    public String getClientTrustCertCollectionPath() {
        return clientTrustCertCollectionPath;
    }

    public void setClientTrustCertCollectionPath(String clientTrustCertCollectionPath) {
        this.clientTrustCertCollectionPath = clientTrustCertCollectionPath;
    }
}
