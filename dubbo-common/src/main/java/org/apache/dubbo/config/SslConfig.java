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
import org.apache.dubbo.common.utils.IOUtils;
import org.apache.dubbo.config.support.Parameter;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.beans.Transient;
import java.io.IOException;
import java.io.InputStream;

public class SslConfig extends AbstractConfig {

    private static final Logger logger = LoggerFactory.getLogger(SslConfig.class);

    public static final String SERVER_KEY_CERT_CHAIN_PATH = "server-key-cert-chain-path";

    public static final String SERVER_PRIVATE_KEY_PATH = "server-private-key-path";

    public static final String SERVER_KEY_PASSWORD = "server-key-password";

    public static final String SERVER_TRUST_CERT_COLLECTION_PATH = "server-trust-cert-collection-path";

    public static final String CLIENT_KEY_CERT_CHAIN_PATH = "client-key-cert-chain-path";

    public static final String CLIENT_PRIVATE_KEY_PATH = "client-private-key-path";

    public static final String CLIENT_KEY_PASSWORD = "client-key-password";

    public static final String CLIENT_TRUST_CERT_COLLECTION_PATH = "client-trust-cert-collection-path";

    private String serverKeyCertChainPath;
    private String serverPrivateKeyPath;
    private String serverKeyPassword;
    private String serverTrustCertCollectionPath;

    private String clientKeyCertChainPath;
    private String clientPrivateKeyPath;
    private String clientKeyPassword;
    private String clientTrustCertCollectionPath;

    private InputStream serverKeyCertChainPathStream;
    private InputStream serverPrivateKeyPathStream;
    private InputStream serverTrustCertCollectionPathStream;

    private InputStream clientKeyCertChainPathStream;
    private InputStream clientPrivateKeyPathStream;
    private InputStream clientTrustCertCollectionPathStream;

    private String caAddress;
    private String envType;
    private String caCertPath;
    private String oidcTokenPath;

    public SslConfig() {
    }

    public SslConfig(ApplicationModel applicationModel) {
        super(applicationModel);
    }

    @Parameter(key = SERVER_KEY_CERT_CHAIN_PATH)
    public String getServerKeyCertChainPath() {
        return serverKeyCertChainPath;
    }

    public void setServerKeyCertChainPath(String serverKeyCertChainPath) {
        this.serverKeyCertChainPath = serverKeyCertChainPath;
    }

    @Parameter(key = SERVER_PRIVATE_KEY_PATH)
    public String getServerPrivateKeyPath() {
        return serverPrivateKeyPath;
    }

    public void setServerPrivateKeyPath(String serverPrivateKeyPath) {
        this.serverPrivateKeyPath = serverPrivateKeyPath;
    }

    @Parameter(key = SERVER_KEY_PASSWORD)
    public String getServerKeyPassword() {
        return serverKeyPassword;
    }

    public void setServerKeyPassword(String serverKeyPassword) {
        this.serverKeyPassword = serverKeyPassword;
    }

    @Parameter(key = SERVER_TRUST_CERT_COLLECTION_PATH)
    public String getServerTrustCertCollectionPath() {
        return serverTrustCertCollectionPath;
    }

    public void setServerTrustCertCollectionPath(String serverTrustCertCollectionPath) {
        this.serverTrustCertCollectionPath = serverTrustCertCollectionPath;
    }

    @Parameter(key = CLIENT_KEY_CERT_CHAIN_PATH)
    public String getClientKeyCertChainPath() {
        return clientKeyCertChainPath;
    }

    public void setClientKeyCertChainPath(String clientKeyCertChainPath) {
        this.clientKeyCertChainPath = clientKeyCertChainPath;
    }

    @Parameter(key = CLIENT_PRIVATE_KEY_PATH)
    public String getClientPrivateKeyPath() {
        return clientPrivateKeyPath;
    }

    public void setClientPrivateKeyPath(String clientPrivateKeyPath) {
        this.clientPrivateKeyPath = clientPrivateKeyPath;
    }

    @Parameter(key = CLIENT_KEY_PASSWORD)
    public String getClientKeyPassword() {
        return clientKeyPassword;
    }

    public void setClientKeyPassword(String clientKeyPassword) {
        this.clientKeyPassword = clientKeyPassword;
    }

    @Parameter(key = CLIENT_TRUST_CERT_COLLECTION_PATH)
    public String getClientTrustCertCollectionPath() {
        return clientTrustCertCollectionPath;
    }

    public void setClientTrustCertCollectionPath(String clientTrustCertCollectionPath) {
        this.clientTrustCertCollectionPath = clientTrustCertCollectionPath;
    }

    public String getCaAddress() {
        return caAddress;
    }

    public void setCaAddress(String caAddress) {
        this.caAddress = caAddress;
    }

    public String getEnvType() {
        return envType;
    }

    public void setEnvType(String envType) {
        this.envType = envType;
    }

    public String getCaCertPath() {
        return caCertPath;
    }

    public void setCaCertPath(String caCertPath) {
        this.caCertPath = caCertPath;
    }

    public String getOidcTokenPath() {
        return oidcTokenPath;
    }

    public void setOidcTokenPath(String oidcTokenPath) {
        this.oidcTokenPath = oidcTokenPath;
    }

    @Transient
    public InputStream getServerKeyCertChainPathStream() throws IOException {
        if (serverKeyCertChainPath != null) {
            serverKeyCertChainPathStream = IOUtils.getURL(serverKeyCertChainPath).openStream();
        }
        return serverKeyCertChainPathStream;
    }

    public void setServerKeyCertChainPathStream(InputStream serverKeyCertChainPathStream) {
        this.serverKeyCertChainPathStream = serverKeyCertChainPathStream;
    }

    @Transient
    public InputStream getServerPrivateKeyPathStream() throws IOException {
        if (serverPrivateKeyPath != null) {
            serverPrivateKeyPathStream = IOUtils.getURL(serverPrivateKeyPath).openStream();
        }
        return serverPrivateKeyPathStream;
    }

    public void setServerPrivateKeyPathStream(InputStream serverPrivateKeyPathStream) {
        this.serverPrivateKeyPathStream = serverPrivateKeyPathStream;
    }

    @Transient
    public InputStream getServerTrustCertCollectionPathStream() throws IOException {
        if (serverTrustCertCollectionPath != null) {
            serverTrustCertCollectionPathStream = IOUtils.getURL(serverTrustCertCollectionPath).openStream();
        }
        return serverTrustCertCollectionPathStream;
    }

    public void setServerTrustCertCollectionPathStream(InputStream serverTrustCertCollectionPathStream) {
        this.serverTrustCertCollectionPathStream = serverTrustCertCollectionPathStream;
    }

    @Transient
    public InputStream getClientKeyCertChainPathStream() throws IOException {
        if (clientKeyCertChainPath != null) {
            clientKeyCertChainPathStream = IOUtils.getURL(clientKeyCertChainPath).openStream();
        }
        return clientKeyCertChainPathStream;
    }

    public void setClientKeyCertChainPathStream(InputStream clientKeyCertChainPathStream) {
        this.clientKeyCertChainPathStream = clientKeyCertChainPathStream;
    }

    @Transient
    public InputStream getClientPrivateKeyPathStream() throws IOException {
        if (clientPrivateKeyPath != null) {
            clientPrivateKeyPathStream = IOUtils.getURL(clientPrivateKeyPath).openStream();
        }
        return clientPrivateKeyPathStream;
    }

    public void setClientPrivateKeyPathStream(InputStream clientPrivateKeyPathStream) {
        this.clientPrivateKeyPathStream = clientPrivateKeyPathStream;
    }

    @Transient
    public InputStream getClientTrustCertCollectionPathStream() throws IOException {
        if (clientTrustCertCollectionPath != null) {
            clientTrustCertCollectionPathStream = IOUtils.getURL(clientTrustCertCollectionPath).openStream();
        }
        return clientTrustCertCollectionPathStream;
    }

    public void setClientTrustCertCollectionPathStream(InputStream clientTrustCertCollectionPathStream) {
        this.clientTrustCertCollectionPathStream = clientTrustCertCollectionPathStream;
    }
}
