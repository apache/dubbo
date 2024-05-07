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

import org.apache.dubbo.xds.listener.DownstreamTlsConfigListener.TlsType;

/**
 * TlsConfig for inbound connection
 */
public class DownstreamTlsConfig {

    private GeneralTlsConfig generalTlsConfig;

    private boolean requireClientCertificate;

    private boolean requireSni;

    private long sessionTimeout;

    private TlsType tlsType;

    public DownstreamTlsConfig(
            GeneralTlsConfig generalTlsConfig,
            boolean requireClientCertificate,
            boolean requireSni,
            long sessionTimeout) {
        this.generalTlsConfig = generalTlsConfig;
        this.requireClientCertificate = requireClientCertificate;
        this.requireSni = requireSni;
        this.sessionTimeout = sessionTimeout;
    }

    public DownstreamTlsConfig(TlsType tlsType) {
        this.tlsType = tlsType;
    }

    public GeneralTlsConfig getGeneralTlsConfig() {
        return generalTlsConfig;
    }

    public void setGeneralTlsConfig(GeneralTlsConfig generalTlsConfig) {
        this.generalTlsConfig = generalTlsConfig;
    }

    public boolean isRequireClientCertificate() {
        return requireClientCertificate;
    }

    public void setRequireClientCertificate(boolean requireClientCertificate) {
        this.requireClientCertificate = requireClientCertificate;
    }

    public boolean isRequireSni() {
        return requireSni;
    }

    public void setRequireSni(boolean requireSni) {
        this.requireSni = requireSni;
    }

    public long getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public void setTlsType(TlsType tlsType) {
        this.tlsType = tlsType;
    }

    public TlsType getTlsType() {
        return tlsType;
    }
}
