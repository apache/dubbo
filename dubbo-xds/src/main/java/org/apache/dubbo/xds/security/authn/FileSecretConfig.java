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

import org.apache.dubbo.common.utils.Pair;
import org.apache.dubbo.xds.security.api.DataSources;

import io.envoyproxy.envoy.config.core.v3.DataSource;

public class FileSecretConfig implements SecretConfig {

    private final String name;

    private final ConfigType configType;

    private final Pair<String, DataSources> certChain;

    private final Pair<String, DataSources> privateKey;

    private final Pair<String, DataSources> password;

    private final Pair<String, DataSources> trust;

    public FileSecretConfig(String name, DataSource certChain, DataSource privateKey, DataSource password) {
        this.name = name;
        this.configType = ConfigType.CERT;
        this.certChain = DataSources.resolveDataSource(certChain);
        this.privateKey = DataSources.resolveDataSource(privateKey);
        if (password != null) {
            this.password = DataSources.resolveDataSource(password);
        } else {
            this.password = null;
        }
        this.trust = null;
    }

    public FileSecretConfig(String name, DataSource certChain, DataSource privateKey) {
        this.name = name;
        this.configType = ConfigType.CERT;
        this.certChain = DataSources.resolveDataSource(certChain);
        this.privateKey = DataSources.resolveDataSource(privateKey);
        this.password = null;
        this.trust = null;
    }

    public FileSecretConfig(String name, DataSource trust) {
        this.name = name;
        this.configType = ConfigType.TRUST;
        this.trust = DataSources.resolveDataSource(trust);
        this.certChain = null;
        this.password = null;
        this.privateKey = null;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public ConfigType configType() {
        return configType;
    }

    @Override
    public Source source() {
        return Source.LOCAL;
    }

    public String getName() {
        return name;
    }

    public Pair<String, DataSources> getCertChain() {
        return certChain;
    }

    public Pair<String, DataSources> getPrivateKey() {
        return privateKey;
    }

    public Pair<String, DataSources> getPassword() {
        return password;
    }

    public Pair<String, DataSources> getTrust() {
        return trust;
    }

    @Override
    public String toString() {
        return "FileSecret{" + "name='" + name + '\'' + ", configType=" + configType + ", certChain=" + certChain
                + ", privateKey=" + privateKey + ", password=" + password + '}';
    }

    public enum DefaultNames {
        LOCAL_TRUST,
        LOCAL_CERT
    }
}
