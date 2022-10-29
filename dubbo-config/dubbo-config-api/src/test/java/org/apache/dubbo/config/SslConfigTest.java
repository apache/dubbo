/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.config;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class SslConfigTest {

    @Test
    public void testServerKeyCertChainPath() throws Exception {
        SslConfig sslConfig = new SslConfig();
        sslConfig.setServerKeyCertChainPath("test-server-key-cert-chain-path");
        assertThat(sslConfig.getServerKeyCertChainPath(),equalTo("test-server-key-cert-chain-path"));
    }

    @Test
    public void testServerPrivateKeyPath() throws Exception {
        SslConfig sslConfig = new SslConfig();
        sslConfig.setServerPrivateKeyPath("test-server-private-key-path");
        assertThat(sslConfig.getServerPrivateKeyPath(),equalTo("test-server-private-key-path"));
    }

    @Test
    public void testServerKeyPassword() throws Exception {
        SslConfig sslConfig = new SslConfig();
        sslConfig.setServerKeyPassword("test-server-key-password");
        assertThat(sslConfig.getServerKeyPassword(),equalTo("test-server-key-password"));
    }

    @Test
    public void testServerTrustCertCollectionPath() throws Exception {
        SslConfig sslConfig = new SslConfig();
        sslConfig.setServerTrustCertCollectionPath("test-server-trust-cert-collection-path");
        assertThat(sslConfig.getServerTrustCertCollectionPath(),equalTo("test-server-trust-cert-collection-path"));
    }

    @Test
    public void testClientKeyCertChainPath() throws Exception {
        SslConfig sslConfig = new SslConfig();
        sslConfig.setClientKeyCertChainPath("test-client-trust-cert-collection-path");
        assertThat(sslConfig.getClientKeyCertChainPath(),equalTo("test-client-trust-cert-collection-path"));
    }

    @Test
    public void testClientPrivateKeyPath() throws Exception {
        SslConfig sslConfig = new SslConfig();
        sslConfig.setClientPrivateKeyPath("test-client-private-key-path");
        assertThat(sslConfig.getClientPrivateKeyPath(),equalTo("test-client-private-key-path"));
    }

    @Test
    public void testClientKeyPassword() throws Exception {
        SslConfig sslConfig = new SslConfig();
        sslConfig.setClientKeyPassword("test-client-key-password");
        assertThat(sslConfig.getClientKeyPassword(),equalTo("test-client-key-password"));
    }

    @Test
    public void testClientTrustCertCollectionPath() throws Exception {
        SslConfig sslConfig = new SslConfig();
        sslConfig.setClientTrustCertCollectionPath("test-client-trust-cert-collection-path");
        assertThat(sslConfig.getClientTrustCertCollectionPath(),equalTo("test-client-trust-cert-collection-path"));
    }

    @Test
    public void testServerKeyCertChainPathStream() throws Exception {
        SslConfig sslConfig = new SslConfig();
        String string = "test-server-key-cert-chain-path-stream";
        InputStream inputStream = IOUtils.toInputStream(string, StandardCharsets.UTF_8);
        sslConfig.setServerKeyCertChainPathStream(inputStream);
        assertThat(sslConfig.getServerKeyCertChainPathStream(),equalTo(inputStream));
    }

    @Test
    public void testServerPrivateKeyPathStream() throws Exception {
        SslConfig sslConfig = new SslConfig();
        String string = "test-server-private-key-path-stream";
        InputStream inputStream = IOUtils.toInputStream(string, StandardCharsets.UTF_8);
        sslConfig.setServerPrivateKeyPathStream(inputStream);
        assertThat(sslConfig.getServerPrivateKeyPathStream(),equalTo(inputStream));
    }

    @Test
    public void testServerTrustCertCollectionPathStream() throws Exception {
        SslConfig sslConfig = new SslConfig();
        String string = "test-server-trust-cert-collection-path-stream";
        InputStream inputStream = IOUtils.toInputStream(string, StandardCharsets.UTF_8);
        sslConfig.setServerTrustCertCollectionPathStream(inputStream);
        assertThat(sslConfig.getServerTrustCertCollectionPathStream(),equalTo(inputStream));
    }

    @Test
    public void testClientKeyCertChainPathStream() throws Exception {
        SslConfig sslConfig = new SslConfig();
        String string = "test-client-key-cert-chain-path-stream";
        InputStream inputStream = IOUtils.toInputStream(string, StandardCharsets.UTF_8);
        sslConfig.setClientKeyCertChainPathStream(inputStream);
        assertThat(sslConfig.getClientKeyCertChainPathStream(),equalTo(inputStream));
    }

    @Test
    public void testClientPrivateKeyPathStream() throws Exception {
        SslConfig sslConfig = new SslConfig();
        String string = "test-client-private-key-path-stream";
        InputStream inputStream = IOUtils.toInputStream(string, StandardCharsets.UTF_8);
        sslConfig.setClientPrivateKeyPathStream(inputStream);
        assertThat(sslConfig.getClientPrivateKeyPathStream(),equalTo(inputStream));
    }

    @Test
    public void testClientTrustCertCollectionPathStream() throws Exception {
        SslConfig sslConfig = new SslConfig();
        String string = "test-client-trust-cert-collection-path-stream";
        InputStream inputStream = IOUtils.toInputStream(string, StandardCharsets.UTF_8);
        sslConfig.setClientTrustCertCollectionPathStream(inputStream);
        assertThat(sslConfig.getClientTrustCertCollectionPathStream(),equalTo(inputStream));
    }

}
