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
package org.apache.dubbo.xds.listener;

import org.apache.dubbo.xds.security.authn.DownstreamTlsConfig;
import org.apache.dubbo.xds.security.authn.UpstreamTlsConfig;

import java.util.Collections;
import java.util.Map;

public class XdsTlsConfigRepository {

    public XdsTlsConfigRepository() {}

    /**
     * inbound ports -> configs
     */
    private volatile Map<String, DownstreamTlsConfig> downstreamConfigs = Collections.emptyMap();

    /**
     * clusterName -> configs
     */
    private volatile Map<String, UpstreamTlsConfig> upstreamConfigs = Collections.emptyMap();

    public void updateInbound(Map<String, DownstreamTlsConfig> downstreamType) {
        this.downstreamConfigs = downstreamType;
    }

    public void updateOutbound(Map<String, UpstreamTlsConfig> upstreamType) {
        this.upstreamConfigs = upstreamType;
    }

    public DownstreamTlsConfig getDownstreamConfig(String port) {
        return downstreamConfigs.get(port);
    }

    public UpstreamTlsConfig getUpstreamConfig(String clusterName) {
        return upstreamConfigs.get(clusterName);
    }
}
