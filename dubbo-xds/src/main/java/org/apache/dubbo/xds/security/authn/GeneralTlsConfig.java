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

import java.util.Collections;
import java.util.List;

public class GeneralTlsConfig {

    /**
     * Name to identify this config, like port or cluster name
     */
    private String name;

    private List<SecretConfig> certConfigs;

    private List<SecretConfig> trustConfigs;

    /**
     * L7 protocols
     */
    private List<String> alpnProtocols;

    public GeneralTlsConfig(String name) {
        this.name = name;
        this.certConfigs = Collections.emptyList();
        this.trustConfigs = Collections.emptyList();
        this.alpnProtocols = Collections.emptyList();
    }

    public GeneralTlsConfig(
            String name, List<SecretConfig> certConfigs, List<SecretConfig> trustConfigs, List<String> alpnProtocols) {
        this.name = name;
        this.certConfigs = certConfigs;
        this.trustConfigs = trustConfigs;
        this.alpnProtocols = alpnProtocols;
    }

    public String getName() {
        return name;
    }

    public List<SecretConfig> certConfigs() {
        return certConfigs;
    }

    public List<SecretConfig> trustConfigs() {
        return trustConfigs;
    }

    public List<String> alpnProtocols() {
        return alpnProtocols;
    }
}
