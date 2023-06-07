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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class IdentityInfo {
    private final String privateKey;
    private final String certificate;
    private final String trustCerts;
    private final long refreshTime;
    private final long expireTime;
    private final String token;
    private final List<String> trustedTokenPublicKeys;

    protected IdentityInfo(String privateKey, String certificate, String trustCerts, long refreshTime, long expireTime, String token, List<String> trustedTokenPublicKeys) {
        this.privateKey = privateKey;
        this.certificate = certificate;
        this.trustCerts = trustCerts;
        this.refreshTime = refreshTime;
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

    public long getRefreshTime() {
        return refreshTime;
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

    public boolean needRefresh() {
        return System.currentTimeMillis() > refreshTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdentityInfo that = (IdentityInfo) o;
        return refreshTime == that.refreshTime && expireTime == that.expireTime && Objects.equals(privateKey, that.privateKey) && Objects.equals(certificate, that.certificate) && Objects.equals(trustCerts, that.trustCerts) && Objects.equals(token, that.token) && Objects.equals(trustedTokenPublicKeys, that.trustedTokenPublicKeys);
    }

    @Override
    public int hashCode() {
        return Objects.hash(privateKey, certificate, trustCerts, refreshTime, expireTime, token, trustedTokenPublicKeys);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String privateKey = "";
        private String certificate = "";
        private String trustCerts = "";
        private long refreshTime;
        private long expireTime;
        private String token = "";
        private List<String> trustedTokenPublicKeys = Collections.emptyList();

        private Builder() {
        }

        public Builder setPrivateKey(String privateKey) {
            this.privateKey = privateKey;
            return this;
        }

        public Builder setCertificate(String certificate) {
            this.certificate = certificate;
            return this;
        }

        public Builder setTrustCerts(String trustCerts) {
            this.trustCerts = trustCerts;
            return this;
        }

        public Builder setRefreshTime(long refreshTime) {
            this.refreshTime = refreshTime;
            return this;
        }

        public Builder setExpireTime(long expireTime) {
            this.expireTime = expireTime;
            return this;
        }

        public Builder setToken(String token) {
            this.token = token;
            return this;
        }

        public Builder setTrustedTokenPublicKeys(List<String> trustedTokenPublicKeys) {
            this.trustedTokenPublicKeys = trustedTokenPublicKeys;
            return this;
        }

        public IdentityInfo build() {
            return new IdentityInfo(privateKey, certificate, trustCerts, refreshTime, expireTime, token, trustedTokenPublicKeys);
        }
    }
}
