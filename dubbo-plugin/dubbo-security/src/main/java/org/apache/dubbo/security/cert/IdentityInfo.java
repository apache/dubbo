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

import java.util.List;
import java.util.Objects;

public class IdentityInfo {
    private final String privateKey;
    private final String certificate;
    private final String trustCerts;
    private final long expireTime;
    private final String token;
    private final List<String> trustedTokenPublicKeys;

    public IdentityInfo(String privateKey, String certificate, String trustCerts, long expireTime, String token, List<String> trustedTokenPublicKeys) {
        this.privateKey = privateKey;
        this.certificate = certificate;
        this.trustCerts = trustCerts;
        this.expireTime = expireTime;
        this.token = token;
        this.trustedTokenPublicKeys = trustedTokenPublicKeys;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public String getCertificate() {
        return certificate;
    }

    public String getTrustCerts() {
        return trustCerts;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public String getToken() {
        return token;
    }

    public List<String> getTrustedTokenPublicKeys() {
        return trustedTokenPublicKeys;
    }

    public boolean isExpire() {
        return System.currentTimeMillis() > expireTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IdentityInfo identityInfo = (IdentityInfo) o;
        return expireTime == identityInfo.expireTime && Objects.equals(privateKey, identityInfo.privateKey) && Objects.equals(certificate, identityInfo.certificate) && Objects.equals(trustCerts, identityInfo.trustCerts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(privateKey, certificate, trustCerts, expireTime);
    }
}
