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

import io.envoyproxy.envoy.config.core.v3.DataSource;

public class FileSecretConfig implements SecretConfig {

    private final String name;

    private final ConfigType configType;

    private final Pair<String, StorageType> certChain;

    private final Pair<String, StorageType> privateKey;

    private final Pair<String, StorageType> password;

    private final Pair<String, StorageType> trust;

    public FileSecretConfig(String name, DataSource certChain, DataSource privateKey, DataSource password) {
        this.name = name;
        this.configType = ConfigType.CERT;
        this.certChain = StorageType.resolveDataSource(certChain);
        this.privateKey = StorageType.resolveDataSource(privateKey);
        if (password != null) {
            this.password = StorageType.resolveDataSource(password);
        } else {
            this.password = null;
        }
        this.trust = null;
    }

    public FileSecretConfig(String name, DataSource certChain, DataSource privateKey) {
        this.name = name;
        this.configType = ConfigType.CERT;
        this.certChain = StorageType.resolveDataSource(certChain);
        this.privateKey = StorageType.resolveDataSource(privateKey);
        this.password = null;
        this.trust = null;
    }

    public FileSecretConfig(String name, DataSource trust) {
        this.name = name;
        this.configType = ConfigType.TRUST;
        this.trust = StorageType.resolveDataSource(trust);
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

    public Pair<String, StorageType> getCertChain() {
        return certChain;
    }

    public Pair<String, StorageType> getPrivateKey() {
        return privateKey;
    }

    public Pair<String, StorageType> getPassword() {
        return password;
    }

    public Pair<String, StorageType> getTrust() {
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

    public enum StorageType {
        LOCAL_FILE,
        ENVIRONMENT_VARIABLE,
        INLINE_STRING,
        INLINE_BYTES;

        public static Pair<String, StorageType> resolveDataSource(DataSource dataSource) {
            if (dataSource.hasFilename()) {
                return new Pair<>(dataSource.getFilename(), LOCAL_FILE);
            }
            if (dataSource.hasEnvironmentVariable()) {
                return new Pair<>(dataSource.getEnvironmentVariable(), ENVIRONMENT_VARIABLE);
            }
            if (dataSource.hasInlineString()) {
                return new Pair<>(dataSource.getInlineString(), INLINE_STRING);
            }
            if (dataSource.hasInlineBytes()) {
                return new Pair<>(dataSource.getInlineBytes().toStringUtf8(), INLINE_BYTES);
            }
            throw new IllegalArgumentException("Unknown data source type");
        }
    }
}
