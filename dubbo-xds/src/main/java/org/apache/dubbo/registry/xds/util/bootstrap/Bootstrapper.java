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
package org.apache.dubbo.registry.xds.util.bootstrap;

import io.envoyproxy.envoy.config.core.v3.Node;
import io.grpc.ChannelCredentials;
import org.apache.dubbo.registry.xds.XdsInitializationException;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public abstract class Bootstrapper {

    public abstract BootstrapInfo bootstrap() throws XdsInitializationException;

    BootstrapInfo bootstrap(Map<String, ?> rawData) throws XdsInitializationException {
        throw new UnsupportedOperationException();
    }

    public abstract static class ServerInfo {
        public abstract String target();

        abstract ChannelCredentials channelCredentials();

        abstract boolean useProtocolV3();

        abstract boolean ignoreResourceDeletion();

    }

    public abstract static class CertificateProviderInfo {
        public abstract String pluginName();

        public abstract Map<String, ?> config();
    }

    public abstract static class BootstrapInfo {
        public abstract List<ServerInfo> servers();

        public abstract Map<String, CertificateProviderInfo> certProviders();

        public abstract Node node();

        public abstract String serverListenerResourceNameTemplate();

        abstract static class Builder {

            abstract Builder servers(List<ServerInfo> servers);

            abstract Builder node(Node node);

            abstract Builder certProviders(@Nullable Map<String, CertificateProviderInfo> certProviders);

            abstract Builder serverListenerResourceNameTemplate(
                @Nullable String serverListenerResourceNameTemplate);

            abstract BootstrapInfo build();
        }
    }
}
