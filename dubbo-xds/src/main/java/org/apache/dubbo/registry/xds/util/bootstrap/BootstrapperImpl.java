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
import io.grpc.internal.JsonParser;
import io.grpc.internal.JsonUtil;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.registry.xds.XdsInitializationException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class BootstrapperImpl extends Bootstrapper {

    static final String BOOTSTRAP_PATH_SYS_ENV_VAR = "GRPC_XDS_BOOTSTRAP";
    static String bootstrapPathFromEnvVar = System.getenv(BOOTSTRAP_PATH_SYS_ENV_VAR);

    private static final Logger logger = LoggerFactory.getLogger(BootstrapperImpl.class);
    private FileReader reader = LocalFileReader.INSTANCE;

    private static final String SERVER_FEATURE_XDS_V3 = "xds_v3";
    private static final String SERVER_FEATURE_IGNORE_RESOURCE_DELETION = "ignore_resource_deletion";

    public BootstrapInfo bootstrap() throws XdsInitializationException {
        String filePath = bootstrapPathFromEnvVar;
        String fileContent = null;
        if (filePath != null) {
            try {
                fileContent = reader.readFile(filePath);
            } catch (IOException e) {
                throw new XdsInitializationException("Fail to read bootstrap file", e);
            }
        }
        if (fileContent == null) throw new XdsInitializationException("Cannot find bootstrap configuration");

        Map<String, ?> rawBootstrap;
        try {
            rawBootstrap = (Map<String, ?>) JsonParser.parse(fileContent);
        } catch (IOException e) {
            throw new XdsInitializationException("Failed to parse JSON", e);
        }
        return bootstrap(rawBootstrap);
    }

    @Override
    BootstrapInfo bootstrap(Map<String, ?> rawData) throws XdsInitializationException {
        BootstrapInfo.Builder builder = new BootstrapInfoImpl.Builder();

        List<?> rawServerConfigs = JsonUtil.getList(rawData, "xds_servers");
        if (rawServerConfigs == null) {
            throw new XdsInitializationException("Invalid bootstrap: 'xds_servers' does not exist.");
        }
        List<ServerInfo> servers = parseServerInfos(rawServerConfigs);
        builder.servers(servers);

        Node.Builder nodeBuilder = Node.newBuilder();
        Map<String, ?> rawNode = JsonUtil.getObject(rawData, "node");
        if (rawNode != null) {
            String id = JsonUtil.getString(rawNode, "id");
            if (id != null) {
                nodeBuilder.setId(id);
            }
            String cluster = JsonUtil.getString(rawNode, "cluster");
            if (cluster != null) {
                nodeBuilder.setCluster(cluster);
            }
            Map<String, ?> metadata = JsonUtil.getObject(rawNode, "metadata");
            Map<String, ?> rawLocality = JsonUtil.getObject(rawNode, "locality");
        }
        builder.node(nodeBuilder.build());

        Map<String, ?> certProvidersBlob = JsonUtil.getObject(rawData, "certificate_providers");
        if (certProvidersBlob != null) {
            Map<String, CertificateProviderInfo> certProviders = new HashMap<>(certProvidersBlob.size());
            for (String name : certProvidersBlob.keySet()) {
                Map<String, ?> valueMap = JsonUtil.getObject(certProvidersBlob, name);
                String pluginName =
                    checkForNull(JsonUtil.getString(valueMap, "plugin_name"), "plugin_name");
                Map<String, ?> config = checkForNull(JsonUtil.getObject(valueMap, "config"), "config");
                CertificateProviderInfoImpl certificateProviderInfo =
                    new CertificateProviderInfoImpl(pluginName, config);
                certProviders.put(name, certificateProviderInfo);
            }
            builder.certProviders(certProviders);
        }

        return builder.build();
    }

    private static List<ServerInfo> parseServerInfos(List<?> rawServerConfigs)
        throws XdsInitializationException {
        List<ServerInfo> servers = new LinkedList<>();
        List<Map<String, ?>> serverConfigList = JsonUtil.checkObjectList(rawServerConfigs);
        for (Map<String, ?> serverConfig : serverConfigList) {
            String serverUri = JsonUtil.getString(serverConfig, "server_uri");
            if (serverUri == null) {
                throw new XdsInitializationException("Invalid bootstrap: missing 'server_uri'");
            }
            List<?> rawChannelCredsList = JsonUtil.getList(serverConfig, "channel_creds");
            if (rawChannelCredsList == null || rawChannelCredsList.isEmpty()) {
                throw new XdsInitializationException(
                    "Invalid bootstrap: server " + serverUri + " 'channel_creds' required");
            }
            ChannelCredentials channelCredentials =
                parseChannelCredentials(JsonUtil.checkObjectList(rawChannelCredsList), serverUri);
//            if (channelCredentials == null) {
//                throw new XdsInitializationException(
//                    "Server " + serverUri + ": no supported channel credentials found");
//            }

            boolean useProtocolV3 = false;
            boolean ignoreResourceDeletion = false;
            List<String> serverFeatures = JsonUtil.getListOfStrings(serverConfig, "server_features");
            if (serverFeatures != null) {
                useProtocolV3 = serverFeatures.contains(SERVER_FEATURE_XDS_V3);
                ignoreResourceDeletion = serverFeatures.contains(SERVER_FEATURE_IGNORE_RESOURCE_DELETION);
            }
            servers.add(
                new ServerInfoImpl(serverUri, channelCredentials, useProtocolV3, ignoreResourceDeletion));
        }
        return servers;
    }

    void setFileReader(FileReader reader) {
        this.reader = reader;
    }

    /**
     * Reads the content of the file with the given path in the file system.
     */
    interface FileReader {
        String readFile(String path) throws IOException;
    }

    private enum LocalFileReader implements FileReader {
        INSTANCE;

        @Override
        public String readFile(String path) throws IOException {
            return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
        }
    }

    private static <T> T checkForNull(T value, String fieldName) throws XdsInitializationException {
        if (value == null) {
            throw new XdsInitializationException(
                "Invalid bootstrap: '" + fieldName + "' does not exist.");
        }
        return value;
    }

    @Nullable
    private static ChannelCredentials parseChannelCredentials(List<Map<String, ?>> jsonList, String serverUri) throws XdsInitializationException {
        return null;
    }
}
