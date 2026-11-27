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
package org.apache.dubbo.xds.security;

import java.nio.file.Path;

import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.AggregatedConfigSource;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.ApiVersion;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.ConfigSource;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.listener.v3.Listener;

/** Shares generated test identities and policies with real Registry/Directory integration tests. */
public final class NativeSecurityTestSupport {
    private NativeSecurityTestSupport() {}

    public static void writeIdentity(Path directory) throws Exception {
        XdsSecurityTest.Authority authority = new XdsSecurityTest.Authority();
        InboundSecurityTest.writeIdentity(directory, authority.identity(InboundSecurityTest.PRINCIPAL), authority);
    }

    public static String bootstrap(Path directory, int adsPort) {
        return InboundSecurityTest.bootstrapJson(directory, adsPort, "metadata-port-test");
    }

    public static Cluster cluster(String name, boolean wrongSan) {
        return XdsSecurityTest.cluster(XdsSecurityTest.common(
                        wrongSan ? "spiffe://cluster.local/ns/test/sa/other" : InboundSecurityTest.PRINCIPAL))
                .toBuilder()
                .setName(name)
                .setType(Cluster.DiscoveryType.EDS)
                .setEdsClusterConfig(Cluster.EdsClusterConfig.newBuilder()
                        .setEdsConfig(ConfigSource.newBuilder()
                                .setAds(AggregatedConfigSource.getDefaultInstance())
                                .setResourceApiVersion(ApiVersion.V3)))
                .build();
    }

    public static Listener listener(int port, boolean denied) {
        return InboundSecurityTest.listener(port, denied ? InboundSecurityTest.deny() : InboundSecurityTest.allow());
    }
}
