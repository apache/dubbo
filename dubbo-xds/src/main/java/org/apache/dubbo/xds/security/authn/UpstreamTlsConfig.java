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
package org.apache.dubbo.xds.security.authn;

/**
 * Tls config for outbound request
 */
public class UpstreamTlsConfig {

    public GeneralTlsConfig generalTlsConfig;

    private String sni;

    private boolean allowRenegotiation;

    public UpstreamTlsConfig(GeneralTlsConfig generalTlsConfig, String sni, boolean allowRenegotiation) {
        this.generalTlsConfig = generalTlsConfig;
        this.sni = sni;
        this.allowRenegotiation = allowRenegotiation;
    }

    public UpstreamTlsConfig() {}

    public GeneralTlsConfig getGeneralTlsConfig() {
        return generalTlsConfig;
    }

    public void setGeneralTlsConfig(GeneralTlsConfig generalTlsConfig) {
        this.generalTlsConfig = generalTlsConfig;
    }

    public String getSni() {
        return sni;
    }

    public void setSni(String sni) {
        this.sni = sni;
    }

    public boolean isAllowRenegotiation() {
        return allowRenegotiation;
    }

    public void setAllowRenegotiation(boolean allowRenegotiation) {
        this.allowRenegotiation = allowRenegotiation;
    }
}
