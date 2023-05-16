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
package org.apache.dubbo.common.ssl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class Cert {
    private final byte[] keyCertChain;
    private final byte[] privateKey;
    private final byte[] trustCert;

    private final String password;

    public Cert(byte[] keyCertChain, byte[] privateKey, byte[] trustCert) {
        this(keyCertChain, privateKey, trustCert, null);
    }

    public Cert(byte[] keyCertChain, byte[] privateKey, byte[] trustCert, String password) {
        this.keyCertChain = keyCertChain;
        this.privateKey = privateKey;
        this.trustCert = trustCert;
        this.password = password;
    }

    public byte[] getKeyCertChain() {
        return keyCertChain;
    }

    public InputStream getKeyCertChainInputStream() {
        return keyCertChain != null ? new ByteArrayInputStream(keyCertChain) : null;
    }

    public byte[] getPrivateKey() {
        return privateKey;
    }

    public InputStream getPrivateKeyInputStream() {
        return privateKey != null ? new ByteArrayInputStream(privateKey) : null;
    }

    public byte[] getTrustCert() {
        return trustCert;
    }

    public InputStream getTrustCertInputStream() {
        return trustCert != null ? new ByteArrayInputStream(trustCert) : null;
    }

    public String getPassword() {
        return password;
    }
}
