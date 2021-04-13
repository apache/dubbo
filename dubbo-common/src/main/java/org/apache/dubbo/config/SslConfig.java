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

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public class SslConfig extends AbstractConfig {

    private static final Logger logger = LoggerFactory.getLogger(SslConfig.class);
    private AtomicBoolean inited = new AtomicBoolean(false);

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

    @Parameter(key = "server-key-cert-chain-path")
    public String getServerKeyCertChainPath() {
        return serverKeyCertChainPath;
    }

    public void setServerKeyCertChainPath(String serverKeyCertChainPath) {
        this.serverKeyCertChainPath = serverKeyCertChainPath;
    }

    @Parameter(key = "server-private-key-path")
    public String getServerPrivateKeyPath() {
        return serverPrivateKeyPath;
    }

    public void setServerPrivateKeyPath(String serverPrivateKeyPath) {
        this.serverPrivateKeyPath = serverPrivateKeyPath;
    }

    @Parameter(key = "server-key-password")
    public String getServerKeyPassword() {
        return serverKeyPassword;
    }

    public void setServerKeyPassword(String serverKeyPassword) {
        this.serverKeyPassword = serverKeyPassword;
    }

    @Parameter(key = "server-trust-cert-collection-path")
    public String getServerTrustCertCollectionPath() {
        return serverTrustCertCollectionPath;
    }

    public void setServerTrustCertCollectionPath(String serverTrustCertCollectionPath) {
        this.serverTrustCertCollectionPath = serverTrustCertCollectionPath;
    }

    @Parameter(key = "client-key-cert-chain-path")
    public String getClientKeyCertChainPath() {
        return clientKeyCertChainPath;
    }

    public void setClientKeyCertChainPath(String clientKeyCertChainPath) {
        this.clientKeyCertChainPath = clientKeyCertChainPath;
    }

    @Parameter(key = "client-private-key-path")
    public String getClientPrivateKeyPath() {
        return clientPrivateKeyPath;
    }

    public void setClientPrivateKeyPath(String clientPrivateKeyPath) {
        this.clientPrivateKeyPath = clientPrivateKeyPath;
    }

    @Parameter(key = "client-key-password")
    public String getClientKeyPassword() {
        return clientKeyPassword;
    }

    public void setClientKeyPassword(String clientKeyPassword) {
        this.clientKeyPassword = clientKeyPassword;
    }

    @Parameter(key = "client-trust-cert-collection-path")
    public String getClientTrustCertCollectionPath() {
        return clientTrustCertCollectionPath;
    }

    public void setClientTrustCertCollectionPath(String clientTrustCertCollectionPath) {
        this.clientTrustCertCollectionPath = clientTrustCertCollectionPath;
    }

    public InputStream getServerKeyCertChainPathStream() throws IOException {
        if (serverKeyCertChainPath != null) {
            serverKeyCertChainPathStream = IOUtils.getURL(serverKeyCertChainPath).openStream();
        }
        return serverKeyCertChainPathStream;
    }

    public void setServerKeyCertChainPathStream(InputStream serverKeyCertChainPathStream) {
        this.serverKeyCertChainPathStream = serverKeyCertChainPathStream;
    }

    public InputStream getServerPrivateKeyPathStream() throws IOException {
        if (serverPrivateKeyPath != null) {
            serverPrivateKeyPathStream = IOUtils.getURL(serverPrivateKeyPath).openStream();
        }
        return serverPrivateKeyPathStream;
    }

    public void setServerPrivateKeyPathStream(InputStream serverPrivateKeyPathStream) {
        this.serverPrivateKeyPathStream = serverPrivateKeyPathStream;
    }

    public InputStream getServerTrustCertCollectionPathStream() throws IOException {
        if (serverTrustCertCollectionPath != null) {
            serverTrustCertCollectionPathStream = IOUtils.getURL(serverTrustCertCollectionPath).openStream();
        }
        return serverTrustCertCollectionPathStream;
    }

    public void setServerTrustCertCollectionPathStream(InputStream serverTrustCertCollectionPathStream) {
        this.serverTrustCertCollectionPathStream = serverTrustCertCollectionPathStream;
    }

    public InputStream getClientKeyCertChainPathStream() throws IOException {
        if (clientKeyCertChainPath != null) {
            clientKeyCertChainPathStream = IOUtils.getURL(clientKeyCertChainPath).openStream();
        }
        return clientKeyCertChainPathStream;
    }

    public void setClientKeyCertChainPathStream(InputStream clientKeyCertChainPathStream) {
        this.clientKeyCertChainPathStream = clientKeyCertChainPathStream;
    }

    public InputStream getClientPrivateKeyPathStream() throws IOException {
        if (clientPrivateKeyPath != null) {
            clientPrivateKeyPathStream = IOUtils.getURL(clientPrivateKeyPath).openStream();
        }
        return clientPrivateKeyPathStream;
    }

    public void setClientPrivateKeyPathStream(InputStream clientPrivateKeyPathStream) {
        this.clientPrivateKeyPathStream = clientPrivateKeyPathStream;
    }

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
