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
package org.apache.dubbo.xds.security.api;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.Pair;
import org.apache.dubbo.xds.security.authn.FileSecretConfig;
import org.apache.dubbo.xds.security.authn.FileSecretConfig.StorageType;
import org.apache.dubbo.xds.security.authn.SecretConfig;
import org.apache.dubbo.xds.security.authn.SecretConfig.ConfigType;
import org.apache.dubbo.xds.security.authn.SecretConfig.Source;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Predicate;

@Activate
public class LocalSecretProvider implements CertSource, TrustSource {

    private ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());

    private FileWatcher watcher = new FileWatcher();

    @Override
    public CertPair getCert(URL url, SecretConfig secretConfig) {

        if (!(secretConfig instanceof FileSecretConfig)) {
            throw new IllegalStateException("Given config not a FileSecret:" + secretConfig);
        }

        FileSecretConfig fileSecretConfig = (FileSecretConfig) secretConfig;

        if (fileSecretConfig.getCertChain() == null) {
            throw new IllegalStateException("CertChain can't be null:" + secretConfig);
        }
        if (fileSecretConfig.getPrivateKey() == null) {
            throw new IllegalStateException("PrivateKey can't be null:" + secretConfig);
        }

        String certChain = readByType(fileSecretConfig.getCertChain());
        String privateKey = readByType(fileSecretConfig.getPrivateKey());
        String password;
        if (fileSecretConfig.getPassword() != null) {
            password = readByType(fileSecretConfig.getPassword());
        } else {
            password = null;
        }
        // TODO how to determine expire time
        return new CertPair(certChain, privateKey, password, System.currentTimeMillis(), Long.MAX_VALUE);
    }

    @Override
    public SecretConfig selectSupportedCertConfig(URL url, List<SecretConfig> secretConfigs) {
        return selectSupportedConfig(
                secretConfigs,
                secretConfig -> ConfigType.CERT.equals(secretConfig.configType())
                        && Source.LOCAL.equals(secretConfig.source()));
    }

    @Override
    public SecretConfig selectSupportedTrustConfig(URL url, List<SecretConfig> secretConfigs) {
        return selectSupportedConfig(
                secretConfigs,
                secretConfig -> ConfigType.TRUST.equals(secretConfig.configType())
                        && Source.LOCAL.equals(secretConfig.source()));
    }

    @Override
    public X509CertChains getTrustCerts(URL url, SecretConfig secretConfig) {

        if (!(secretConfig instanceof FileSecretConfig)) {
            throw new IllegalStateException("Given config not a FileSecret:" + secretConfig);
        }
        FileSecretConfig config = (FileSecretConfig) secretConfig;

        if (config.getTrust() == null) {
            throw new IllegalStateException("Trust can't be null:" + secretConfig);
        }
        String trust = readByType(config.getTrust());
        // TODO how to determine expire time
        return new X509CertChains(trust, System.currentTimeMillis(), Long.MAX_VALUE);
    }

    private SecretConfig selectSupportedConfig(List<SecretConfig> secretConfig, Predicate<SecretConfig> selector) {
        SecretConfig config = secretConfig.stream().filter(selector).findFirst().orElse(null);
        if (config == null) {
            return null;
        }
        FileSecretConfig secret = (FileSecretConfig) config;
        try {
            if (StorageType.LOCAL_FILE.equals(secret.getCertChain().getValue())) {
                watcher.registerWatch(secret.getCertChain().getKey());
            }
            if (StorageType.LOCAL_FILE.equals(secret.getPrivateKey().getValue())) {
                watcher.registerWatch(secret.getPrivateKey().getKey());
            }
            if (secret.getPassword() != null
                    && StorageType.LOCAL_FILE.equals(secret.getPassword().getValue())) {
                watcher.registerWatch(secret.getPassword().getKey());
            }
        } catch (Exception e) {
            logger.warn(
                    "",
                    "",
                    "",
                    "Failed to watch local file secrets, SecretConfig are removed from list. config=" + secret,
                    e);
            secretConfig.remove(config);
            selectSupportedConfig(secretConfig, selector);
        }
        return secret;
    }

    private String readByType(Pair<String, StorageType> pair) {
        switch (pair.getRight()) {
            case LOCAL_FILE:
                return new String(watcher.readWatchedFile(pair.getLeft()), StandardCharsets.UTF_8);
            case INLINE_BYTES:
                return pair.getLeft();
            case INLINE_STRING:
                return pair.getLeft();
            case ENVIRONMENT_VARIABLE:
                return System.getenv(pair.getLeft());
            default:
                throw new IllegalArgumentException("Unknown storage type:" + pair.getRight());
        }
    }
}
