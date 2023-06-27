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

import java.util.Objects;

import static org.apache.dubbo.security.cert.Constants.DEFAULT_REFRESH_INTERVAL;

public class CertConfig {
    private final String remoteAddress;
    private final String envType;
    private final String caCertPath;
    /**
     * Path to OpenID Connect Token file
     */
    private final String oidcTokenPath;

    private final int refreshInterval;

    public CertConfig(String remoteAddress, String envType, String caCertPath, String oidcTokenPath) {
        this(remoteAddress, envType, caCertPath, oidcTokenPath, DEFAULT_REFRESH_INTERVAL);
    }

    public CertConfig(String remoteAddress, String envType, String caCertPath, String oidcTokenPath, int refreshInterval) {
        this.remoteAddress = remoteAddress;
        this.envType = envType;
        this.caCertPath = caCertPath;
        this.oidcTokenPath = oidcTokenPath;
        this.refreshInterval = refreshInterval;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public String getEnvType() {
        return envType;
    }

    public String getCaCertPath() {
        return caCertPath;
    }

    public String getOidcTokenPath() {
        return oidcTokenPath;
    }

    public int getRefreshInterval() {
        return refreshInterval;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CertConfig that = (CertConfig) o;
        return Objects.equals(remoteAddress, that.remoteAddress) && Objects.equals(envType, that.envType) && Objects.equals(caCertPath, that.caCertPath) && Objects.equals(oidcTokenPath, that.oidcTokenPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(remoteAddress, envType, caCertPath, oidcTokenPath);
    }
}
