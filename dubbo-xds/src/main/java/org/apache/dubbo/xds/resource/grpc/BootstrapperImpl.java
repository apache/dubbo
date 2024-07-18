/*
 * Copyright 2021 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.xds.resource.grpc;

import org.apache.dubbo.xds.XdsInitializationException;
import org.apache.dubbo.xds.resource.grpc.EnvoyProtoData.Node;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.grpc.ChannelCredentials;
import io.grpc.InternalLogId;
import io.grpc.internal.GrpcUtil;
import io.grpc.internal.GrpcUtil.GrpcBuildVersion;
import io.grpc.internal.JsonParser;
import io.grpc.internal.JsonUtil;

import javax.annotation.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link Bootstrapper} implementation that reads xDS configurations from local file system.
 */
class BootstrapperImpl extends Bootstrapper {

  private static final String BOOTSTRAP_PATH_SYS_ENV_VAR = "GRPC_XDS_BOOTSTRAP";
  @VisibleForTesting
  static String bootstrapPathFromEnvVar = System.getenv(BOOTSTRAP_PATH_SYS_ENV_VAR);
  private static final String BOOTSTRAP_PATH_SYS_PROPERTY = "io.grpc.xds.bootstrap";
  @VisibleForTesting
  static String bootstrapPathFromSysProp = System.getProperty(BOOTSTRAP_PATH_SYS_PROPERTY);
  private static final String BOOTSTRAP_CONFIG_SYS_ENV_VAR = "GRPC_XDS_BOOTSTRAP_CONFIG";
  @VisibleForTesting
  static String bootstrapConfigFromEnvVar = System.getenv(BOOTSTRAP_CONFIG_SYS_ENV_VAR);
  private static final String BOOTSTRAP_CONFIG_SYS_PROPERTY = "io.grpc.xds.bootstrapConfig";
  @VisibleForTesting
  static String bootstrapConfigFromSysProp = System.getProperty(BOOTSTRAP_CONFIG_SYS_PROPERTY);

  // Feature-gating environment variables.
  static boolean enableFederation =
      Strings.isNullOrEmpty(System.getenv("GRPC_EXPERIMENTAL_XDS_FEDERATION"))
          || Boolean.parseBoolean(System.getenv("GRPC_EXPERIMENTAL_XDS_FEDERATION"));

  // Client features.
  @VisibleForTesting
  static final String CLIENT_FEATURE_DISABLE_OVERPROVISIONING =
      "envoy.lb.does_not_support_overprovisioning";
  @VisibleForTesting
  static final String CLIENT_FEATURE_RESOURCE_IN_SOTW = "xds.config.resource-in-sotw";

  // Server features.
  private static final String SERVER_FEATURE_IGNORE_RESOURCE_DELETION = "ignore_resource_deletion";

//  private final XdsLogger logger;
  private FileReader reader = LocalFileReader.INSTANCE;

  public BootstrapperImpl() {
//    logger = XdsLogger.withLogId(InternalLogId.allocate("bootstrapper", null));
  }

  /**
   * Reads and parses bootstrap config. Searches the config (or file of config) with the
   * following order:
   *
   * <ol>
   *   <li>A filesystem path defined by environment variable "GRPC_XDS_BOOTSTRAP"</li>
   *   <li>A filesystem path defined by Java System Property "io.grpc.xds.bootstrap"</li>
   *   <li>Environment variable value of "GRPC_XDS_BOOTSTRAP_CONFIG"</li>
   *   <li>Java System Property value of "io.grpc.xds.bootstrapConfig"</li>
   * </ol>
   */
  @SuppressWarnings("unchecked")
  @Override
  public BootstrapInfo bootstrap() throws XdsInitializationException {
    String filePath =
        bootstrapPathFromEnvVar != null ? bootstrapPathFromEnvVar : bootstrapPathFromSysProp;
    String fileContent;
    if (filePath != null) {
//      logger.log(XdsLogLevel.INFO, "Reading bootstrap file from {0}", filePath);
      try {
        fileContent = reader.readFile(filePath);
      } catch (IOException e) {
        throw new XdsInitializationException("Fail to read bootstrap file", e);
      }
    } else {
      fileContent = bootstrapConfigFromEnvVar != null
          ? bootstrapConfigFromEnvVar : bootstrapConfigFromSysProp;
    }
    if (fileContent == null) {
      throw new XdsInitializationException(
          "Cannot find bootstrap configuration\n"
              + "Environment variables searched:\n"
              + "- " + BOOTSTRAP_PATH_SYS_ENV_VAR + "\n"
              + "- " + BOOTSTRAP_CONFIG_SYS_ENV_VAR + "\n\n"
              + "Java System Properties searched:\n"
              + "- " + BOOTSTRAP_PATH_SYS_PROPERTY + "\n"
              + "- " + BOOTSTRAP_CONFIG_SYS_PROPERTY + "\n\n");
    }

//    logger.log(XdsLogLevel.INFO, "Reading bootstrap from " + filePath);
    Map<String, ?> rawBootstrap;
    try {
      rawBootstrap = (Map<String, ?>) JsonParser.parse(fileContent);
    } catch (IOException e) {
      throw new XdsInitializationException("Failed to parse JSON", e);
    }
//    logger.log(XdsLogLevel.DEBUG, "Bootstrap configuration:\n{0}", rawBootstrap);
    return bootstrap(rawBootstrap);
  }

  @Override
  BootstrapInfo bootstrap(Map<String, ?> rawData) throws XdsInitializationException {
    BootstrapInfo.Builder builder = BootstrapInfo.builder();

    List<?> rawServerConfigs = JsonUtil.getList(rawData, "xds_servers");
    if (rawServerConfigs == null) {
      throw new XdsInitializationException("Invalid bootstrap: 'xds_servers' does not exist.");
    }
    List<ServerInfo> servers = parseServerInfos(rawServerConfigs/*, logger*/);
    builder.servers(servers);

    Node.Builder nodeBuilder = Node.newBuilder();
    Map<String, ?> rawNode = JsonUtil.getObject(rawData, "node");
    if (rawNode != null) {
      String id = JsonUtil.getString(rawNode, "id");
      if (id != null) {
//        logger.log(XdsLogLevel.INFO, "Node id: {0}", id);
        nodeBuilder.setId(id);
      }
      String cluster = JsonUtil.getString(rawNode, "cluster");
      if (cluster != null) {
//        logger.log(XdsLogLevel.INFO, "Node cluster: {0}", cluster);
        nodeBuilder.setCluster(cluster);
      }
      Map<String, ?> metadata = JsonUtil.getObject(rawNode, "metadata");
      if (metadata != null) {
        nodeBuilder.setMetadata(metadata);
      }
      Map<String, ?> rawLocality = JsonUtil.getObject(rawNode, "locality");
      if (rawLocality != null) {
        String region = "";
        String zone = "";
        String subZone = "";
        if (rawLocality.containsKey("region")) {
          region = JsonUtil.getString(rawLocality, "region");
        }
        if (rawLocality.containsKey("zone")) {
          zone = JsonUtil.getString(rawLocality, "zone");
        }
        if (rawLocality.containsKey("sub_zone")) {
          subZone = JsonUtil.getString(rawLocality, "sub_zone");
        }
//        logger.log(XdsLogLevel.INFO, "Locality region: {0}, zone: {1}, subZone: {2}",
//            region, zone, subZone);
        Locality locality = Locality.create(region, zone, subZone);
        nodeBuilder.setLocality(locality);
      }
    }
    GrpcBuildVersion buildVersion = GrpcUtil.getGrpcBuildVersion();
//    logger.log(XdsLogLevel.INFO, "Build version: {0}", buildVersion);
    nodeBuilder.setBuildVersion(buildVersion.toString());
    nodeBuilder.setUserAgentName(buildVersion.getUserAgent());
    nodeBuilder.setUserAgentVersion(buildVersion.getImplementationVersion());
    nodeBuilder.addClientFeatures(CLIENT_FEATURE_DISABLE_OVERPROVISIONING);
    nodeBuilder.addClientFeatures(CLIENT_FEATURE_RESOURCE_IN_SOTW);
//    builder.node(nodeBuilder.build());

    Map<String, ?> certProvidersBlob = JsonUtil.getObject(rawData, "certificate_providers");
    if (certProvidersBlob != null) {
//      logger.log(XdsLogLevel.INFO, "Configured with {0} cert providers", certProvidersBlob.size());
      Map<String, CertificateProviderInfo> certProviders = new HashMap<>(certProvidersBlob.size());
      for (String name : certProvidersBlob.keySet()) {
        Map<String, ?> valueMap = JsonUtil.getObject(certProvidersBlob, name);
        String pluginName =
            checkForNull(JsonUtil.getString(valueMap, "plugin_name"), "plugin_name");
//        logger.log(XdsLogLevel.INFO, "cert provider: {0}, plugin name: {1}", name, pluginName);
        Map<String, ?> config = checkForNull(JsonUtil.getObject(valueMap, "config"), "config");
        CertificateProviderInfo certificateProviderInfo =
            CertificateProviderInfo.create(pluginName, config);
        certProviders.put(name, certificateProviderInfo);
      }
      builder.certProviders(certProviders);
    }

    String grpcServerResourceId =
        JsonUtil.getString(rawData, "server_listener_resource_name_template");
//    logger.log(
//        XdsLogLevel.INFO, "server_listener_resource_name_template: {0}", grpcServerResourceId);
    builder.serverListenerResourceNameTemplate(grpcServerResourceId);

    if (!enableFederation) {
      return builder.build();
    }
    String grpcClientDefaultListener =
        JsonUtil.getString(rawData, "client_default_listener_resource_name_template");
//    logger.log(
//        XdsLogLevel.INFO, "client_default_listener_resource_name_template: {0}",
//        grpcClientDefaultListener);
    if (grpcClientDefaultListener != null) {
      builder.clientDefaultListenerResourceNameTemplate(grpcClientDefaultListener);
    }

    Map<String, ?> rawAuthoritiesMap =
        JsonUtil.getObject(rawData, "authorities");
    ImmutableMap.Builder<String, AuthorityInfo> authorityInfoMapBuilder = ImmutableMap.builder();
    if (rawAuthoritiesMap != null) {
//      logger.log(
//          XdsLogLevel.INFO, "Configured with {0} xDS server authorities", rawAuthoritiesMap.size());
      for (String authorityName : rawAuthoritiesMap.keySet()) {
//        logger.log(XdsLogLevel.INFO, "xDS server authority: {0}", authorityName);
        Map<String, ?> rawAuthority = JsonUtil.getObject(rawAuthoritiesMap, authorityName);
        String clientListnerTemplate =
            JsonUtil.getString(rawAuthority, "client_listener_resource_name_template");
//        logger.log(
//            XdsLogLevel.INFO, "client_listener_resource_name_template: {0}", clientListnerTemplate);
        String prefix = XDSTP_SCHEME + "//" + authorityName + "/";
        if (clientListnerTemplate == null) {
          clientListnerTemplate = prefix + "envoy.config.listener.v3.Listener/%s";
        } else if (!clientListnerTemplate.startsWith(prefix)) {
          throw new XdsInitializationException(
              "client_listener_resource_name_template: '" + clientListnerTemplate
                  + "' does not start with " + prefix);
        }
        List<?> rawAuthorityServers = JsonUtil.getList(rawAuthority, "xds_servers");
        List<ServerInfo> authorityServers;
        if (rawAuthorityServers == null || rawAuthorityServers.isEmpty()) {
          authorityServers = servers;
        } else {
          authorityServers = parseServerInfos(rawAuthorityServers/*, logger*/);
        }
        authorityInfoMapBuilder.put(
            authorityName, AuthorityInfo.create(clientListnerTemplate, authorityServers));
      }
      builder.authorities(authorityInfoMapBuilder.buildOrThrow());
    }

    return builder.build();
  }

  private static List<ServerInfo> parseServerInfos(List<?> rawServerConfigs/*, XdsLogger logger*/)
      throws XdsInitializationException {
//    logger.log(XdsLogLevel.INFO, "Configured with {0} xDS servers", rawServerConfigs.size());
    ImmutableList.Builder<ServerInfo> servers = ImmutableList.builder();
    List<Map<String, ?>> serverConfigList = JsonUtil.checkObjectList(rawServerConfigs);
    for (Map<String, ?> serverConfig : serverConfigList) {
      String serverUri = JsonUtil.getString(serverConfig, "server_uri");
      if (serverUri == null) {
        throw new XdsInitializationException("Invalid bootstrap: missing 'server_uri'");
      }
//      logger.log(XdsLogLevel.INFO, "xDS server URI: {0}", serverUri);

      List<?> rawChannelCredsList = JsonUtil.getList(serverConfig, "channel_creds");
      if (rawChannelCredsList == null || rawChannelCredsList.isEmpty()) {
        throw new XdsInitializationException(
            "Invalid bootstrap: server " + serverUri + " 'channel_creds' required");
      }
      ChannelCredentials channelCredentials =
          parseChannelCredentials(JsonUtil.checkObjectList(rawChannelCredsList), serverUri);
      if (channelCredentials == null) {
        throw new XdsInitializationException(
            "Server " + serverUri + ": no supported channel credentials found");
      }

      boolean ignoreResourceDeletion = false;
      List<String> serverFeatures = JsonUtil.getListOfStrings(serverConfig, "server_features");
      if (serverFeatures != null) {
//        logger.log(XdsLogLevel.INFO, "Server features: {0}", serverFeatures);
        ignoreResourceDeletion = serverFeatures.contains(SERVER_FEATURE_IGNORE_RESOURCE_DELETION);
      }
      servers.add(
          ServerInfo.create(serverUri, channelCredentials, ignoreResourceDeletion));
    }
    return servers.build();
  }

  @VisibleForTesting
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
  private static ChannelCredentials parseChannelCredentials(List<Map<String, ?>> jsonList,
      String serverUri) throws XdsInitializationException {
    for (Map<String, ?> channelCreds : jsonList) {
      String type = JsonUtil.getString(channelCreds, "type");
      if (type == null) {
        throw new XdsInitializationException(
            "Invalid bootstrap: server " + serverUri + " with 'channel_creds' type unspecified");
      }
//      XdsCredentialsProvider provider =  XdsCredentialsRegistry.getDefaultRegistry()
//          .getProvider(type);
//      if (provider != null) {
//        Map<String, ?> config = JsonUtil.getObject(channelCreds, "config");
//        if (config == null) {
//          config = ImmutableMap.of();
//        }
//
//        return provider.newChannelCredentials(config);
//      }
//    }
    return null;
  }
}
