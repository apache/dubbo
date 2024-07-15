/*
 * Copyright 2019 The gRPC Authors
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

package org.apache.dubbo.xds.test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import io.grpc.InsecureChannelCredentials;
import io.grpc.TlsChannelCredentials;
import io.grpc.internal.GrpcUtil;
import io.grpc.internal.GrpcUtil.GrpcBuildVersion;

import org.apache.dubbo.xds.XdsInitializationException;
import org.apache.dubbo.xds.bootstrap.Bootstrapper;
import org.apache.dubbo.xds.bootstrap.Bootstrapper.AuthorityInfo;
import org.apache.dubbo.xds.bootstrap.Bootstrapper.BootstrapInfo;
import org.apache.dubbo.xds.bootstrap.Bootstrapper.ServerInfo;
import org.apache.dubbo.xds.bootstrap.BootstrapperImpl;
import org.apache.dubbo.xds.bootstrap.DubboBootstrapperImpl;

import org.apache.dubbo.xds.bootstrap.EnvoyProtoData.Node;
import org.apache.dubbo.xds.bootstrap.Locality;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/** Unit tests for {@link DubboBootstrapperImpl}. */
@RunWith(JUnit4.class)
public class DubboBootstrapperImplTest {

  private static final String BOOTSTRAP_FILE_PATH = "C:\\Users\\Windows 10\\Desktop\\grpc-bootstrap.json";
  private static final String SERVER_URI = "unix:///etc/istio/proxy/XDS";
  @SuppressWarnings("deprecation") // https://github.com/grpc/grpc-java/issues/7467
  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  private final DubboBootstrapperImpl bootstrapper = new DubboBootstrapperImpl();
  private String originalBootstrapPathFromEnvVar;
  private String originalBootstrapPathFromSysProp;
  private String originalBootstrapConfigFromEnvVar;
  private String originalBootstrapConfigFromSysProp;

  @Before
  public void setUp() {
    saveEnvironment();
    bootstrapper.bootstrapPathFromEnvVar = BOOTSTRAP_FILE_PATH;
  }

  private void saveEnvironment() {
    originalBootstrapPathFromEnvVar = bootstrapper.bootstrapPathFromEnvVar;
    originalBootstrapPathFromSysProp = bootstrapper.bootstrapPathFromSysProp;
    originalBootstrapConfigFromEnvVar = bootstrapper.bootstrapConfigFromEnvVar;
    originalBootstrapConfigFromSysProp = bootstrapper.bootstrapConfigFromSysProp;
  }

  @After
  public void restoreEnvironment() {
    bootstrapper.bootstrapPathFromEnvVar = originalBootstrapPathFromEnvVar;
    bootstrapper.bootstrapPathFromSysProp = originalBootstrapPathFromSysProp;
    bootstrapper.bootstrapConfigFromEnvVar = originalBootstrapConfigFromEnvVar;
    bootstrapper.bootstrapConfigFromSysProp = originalBootstrapConfigFromSysProp;
  }

  @Test
  public void parseBootstrap_singleXdsServer() throws XdsInitializationException {
    //bootstrapper.setFileReader(createFileReader(BOOTSTRAP_FILE_PATH, rawData));
    BootstrapInfo info = bootstrapper.bootstrap();
    assertThat(info.servers()).hasSize(1);
    ServerInfo serverInfo = Iterables.getOnlyElement(info.servers());
    assertThat(serverInfo.target()).isEqualTo(SERVER_URI);
    assertThat(serverInfo.implSpecificConfig()).isInstanceOf(InsecureChannelCredentials.class);
  }

  @Test
  public void parseBootstrap_multipleXdsServers() throws XdsInitializationException {
    String rawData = "{\n"
        + "  \"node\": {\n"
        + "    \"id\": \"ENVOY_NODE_ID\",\n"
        + "    \"cluster\": \"ENVOY_CLUSTER\",\n"
        + "    \"locality\": {\n"
        + "      \"region\": \"ENVOY_REGION\",\n"
        + "      \"zone\": \"ENVOY_ZONE\",\n"
        + "      \"sub_zone\": \"ENVOY_SUBZONE\"\n"
        + "    },\n"
        + "    \"metadata\": {\n"
        + "      \"TRAFFICDIRECTOR_INTERCEPTION_PORT\": \"ENVOY_PORT\",\n"
        + "      \"TRAFFICDIRECTOR_NETWORK_NAME\": \"VPC_NETWORK_NAME\"\n"
        + "    }\n"
        + "  },\n"
        + "  \"xds_servers\": [\n"
        + "    {\n"
        + "      \"server_uri\": \"trafficdirector-foo.googleapis.com:443\",\n"
        + "      \"channel_creds\": [\n"
        + "        {\"type\": \"tls\"}\n"
        + "      ]\n"
        + "    },\n"
        + "    {\n"
        + "      \"server_uri\": \"trafficdirector-bar.googleapis.com:443\",\n"
        + "      \"channel_creds\": [\n"
        + "        {\"type\": \"insecure\"}"
        + "      ]\n"
        + "    }\n"
        + "  ]\n"
        + "}";

    bootstrapper.setFileReader(createFileReader(BOOTSTRAP_FILE_PATH, rawData));
    BootstrapInfo info = bootstrapper.bootstrap();
    assertThat(info.servers()).hasSize(2);
    List<ServerInfo> serverInfoList = info.servers();
    assertThat(serverInfoList.get(0).target())
        .isEqualTo("trafficdirector-foo.googleapis.com:443");
    assertThat(serverInfoList.get(0).implSpecificConfig())
        .isInstanceOf(TlsChannelCredentials.class);
    assertThat(serverInfoList.get(1).target())
        .isEqualTo("trafficdirector-bar.googleapis.com:443");
    assertThat(serverInfoList.get(1).implSpecificConfig())
        .isInstanceOf(InsecureChannelCredentials.class);
    assertThat(info.node()).isEqualTo(
        getNodeBuilder()
            .setId("ENVOY_NODE_ID")
            .setCluster("ENVOY_CLUSTER")
            .setLocality(Locality.create("ENVOY_REGION", "ENVOY_ZONE", "ENVOY_SUBZONE"))
            .setMetadata(
                ImmutableMap.of(
                    "TRAFFICDIRECTOR_INTERCEPTION_PORT",
                    "ENVOY_PORT",
                    "TRAFFICDIRECTOR_NETWORK_NAME",
                    "VPC_NETWORK_NAME"))
            .build());
  }

  @Test
  public void parseBootstrap_IgnoreIrrelevantFields() throws XdsInitializationException {
    String rawData = "{\n"
        + "  \"node\": {\n"
        + "    \"id\": \"ENVOY_NODE_ID\",\n"
        + "    \"cluster\": \"ENVOY_CLUSTER\",\n"
        + "    \"locality\": {},\n"
        + "    \"metadata\": {\n"
        + "      \"TRAFFICDIRECTOR_INTERCEPTION_PORT\": \"ENVOY_PORT\",\n"
        + "      \"TRAFFICDIRECTOR_NETWORK_NAME\": \"VPC_NETWORK_NAME\"\n"
        + "    }\n"
        + "  },\n"
        + "  \"xds_servers\": [\n"
        + "    {\n"
        + "      \"server_uri\": \"" + SERVER_URI + "\",\n"
        + "      \"ignore\": \"something irrelevant\","
        + "      \"channel_creds\": [\n"
        + "        {\"type\": \"insecure\"}\n"
        + "      ]\n"
        + "    }\n"
        + "  ],\n"
        + "  \"ignore\": \"something irrelevant\"\n"
        + "}";

    bootstrapper.setFileReader(createFileReader(BOOTSTRAP_FILE_PATH, rawData));
    BootstrapInfo info = bootstrapper.bootstrap();
    assertThat(info.servers()).hasSize(1);
    ServerInfo serverInfo = Iterables.getOnlyElement(info.servers());
    assertThat(serverInfo.target()).isEqualTo(SERVER_URI);
    assertThat(serverInfo.implSpecificConfig()).isInstanceOf(InsecureChannelCredentials.class);
    assertThat(info.node()).isEqualTo(
        getNodeBuilder()
            .setId("ENVOY_NODE_ID")
            .setCluster("ENVOY_CLUSTER")
            .setLocality(Locality.create("", "", ""))
            .setMetadata(
                ImmutableMap.of(
                    "TRAFFICDIRECTOR_INTERCEPTION_PORT",
                    "ENVOY_PORT",
                    "TRAFFICDIRECTOR_NETWORK_NAME",
                    "VPC_NETWORK_NAME"))
            .build());
  }

  @Test
  public void parseBootstrap_missingServerChannelCreds() throws XdsInitializationException {
    String rawData = "{\n"
        + "  \"xds_servers\": [\n"
        + "    {\n"
        + "      \"server_uri\": \"" + SERVER_URI + "\"\n"
        + "    }\n"
        + "  ]\n"
        + "}";

    bootstrapper.setFileReader(createFileReader(BOOTSTRAP_FILE_PATH, rawData));
    thrown.expect(XdsInitializationException.class);
    thrown.expectMessage("Invalid bootstrap: server " + SERVER_URI + " 'channel_creds' required");
    bootstrapper.bootstrap();
  }

  @Test
  public void parseBootstrap_unsupportedServerChannelCreds() throws XdsInitializationException {
    String rawData = "{\n"
        + "  \"xds_servers\": [\n"
        + "    {\n"
        + "      \"server_uri\": \"" + SERVER_URI + "\",\n"
        + "      \"channel_creds\": [\n"
        + "        {\"type\": \"unsupported\"}\n"
        + "      ]\n"
        + "    }\n"
        + "  ]\n"
        + "}";

    bootstrapper.setFileReader(createFileReader(BOOTSTRAP_FILE_PATH, rawData));
    thrown.expect(XdsInitializationException.class);
    thrown.expectMessage("Server " + SERVER_URI + ": no supported channel credentials found");
    bootstrapper.bootstrap();
  }

  @Test
  public void parseBootstrap_useFirstSupportedChannelCredentials()
      throws XdsInitializationException {
    String rawData = "{\n"
        + "  \"xds_servers\": [\n"
        + "    {\n"
        + "      \"server_uri\": \"" + SERVER_URI + "\",\n"
        + "      \"channel_creds\": [\n"
        + "        {\"type\": \"unsupported\"}, {\"type\": \"insecure\"}, {\"type\": \"tls\"}\n"
        + "      ]\n"
        + "    }\n"
        + "  ]\n"
        + "}";

    bootstrapper.setFileReader(createFileReader(BOOTSTRAP_FILE_PATH, rawData));
    BootstrapInfo info = bootstrapper.bootstrap();
    assertThat(info.servers()).hasSize(1);
    ServerInfo serverInfo = Iterables.getOnlyElement(info.servers());
    assertThat(serverInfo.target()).isEqualTo(SERVER_URI);
    assertThat(serverInfo.implSpecificConfig()).isInstanceOf(InsecureChannelCredentials.class);
    assertThat(info.node()).isEqualTo(getNodeBuilder().build());
  }

  @Test
  public void parseBootstrap_noXdsServers() throws XdsInitializationException {
    String rawData = "{\n"
        + "  \"node\": {\n"
        + "    \"id\": \"ENVOY_NODE_ID\",\n"
        + "    \"cluster\": \"ENVOY_CLUSTER\",\n"
        + "    \"locality\": {\n"
        + "      \"region\": \"ENVOY_REGION\",\n"
        + "      \"zone\": \"ENVOY_ZONE\",\n"
        + "      \"sub_zone\": \"ENVOY_SUBZONE\"\n"
        + "    },\n"
        + "    \"metadata\": {\n"
        + "      \"TRAFFICDIRECTOR_INTERCEPTION_PORT\": \"ENVOY_PORT\",\n"
        + "      \"TRAFFICDIRECTOR_NETWORK_NAME\": \"VPC_NETWORK_NAME\"\n"
        + "    }\n"
        + "  }\n"
        + "}";

    bootstrapper.setFileReader(createFileReader(BOOTSTRAP_FILE_PATH, rawData));
    thrown.expect(XdsInitializationException.class);
    thrown.expectMessage("Invalid bootstrap: 'xds_servers' does not exist.");
    bootstrapper.bootstrap();
  }

  @Test
  public void parseBootstrap_serverWithoutServerUri() throws XdsInitializationException {
    String rawData = "{"
        + "  \"node\": {\n"
        + "    \"id\": \"ENVOY_NODE_ID\",\n"
        + "    \"cluster\": \"ENVOY_CLUSTER\",\n"
        + "    \"locality\": {\n"
        + "      \"region\": \"ENVOY_REGION\",\n"
        + "      \"zone\": \"ENVOY_ZONE\",\n"
        + "      \"sub_zone\": \"ENVOY_SUBZONE\"\n"
        + "    },\n"
        + "    \"metadata\": {\n"
        + "      \"TRAFFICDIRECTOR_INTERCEPTION_PORT\": \"ENVOY_PORT\",\n"
        + "      \"TRAFFICDIRECTOR_NETWORK_NAME\": \"VPC_NETWORK_NAME\"\n"
        + "    }\n"
        + "  },\n"
        + "  \"xds_servers\": [\n"
        + "    {\n"
        + "      \"channel_creds\": [\n"
        + "        {\"type\": \"tls\"}, {\"type\": \"loas\"}\n"
        + "      ]\n"
        + "    }\n"
        + "  ]\n "
        + "}";

    bootstrapper.setFileReader(createFileReader(BOOTSTRAP_FILE_PATH, rawData));
    thrown.expectMessage("Invalid bootstrap: missing 'server_uri'");
    bootstrapper.bootstrap();
  }

  @Test
  public void parseBootstrap_certProviderInstances() throws XdsInitializationException {
    String rawData =
        "{\n"
            + "  \"xds_servers\": [],\n"
            + "  \"certificate_providers\": {\n"
            + "    \"gcp_id\": {\n"
            + "      \"plugin_name\": \"meshca\",\n"
            + "      \"config\": {\n"
            + "        \"server\": {\n"
            + "          \"api_type\": \"GRPC\",\n"
            + "          \"grpc_services\": [{\n"
            + "            \"google_grpc\": {\n"
            + "              \"target_uri\": \"meshca.com\",\n"
            + "              \"channel_credentials\": {\"google_default\": {}},\n"
            + "              \"call_credentials\": [{\n"
            + "                \"sts_service\": {\n"
            + "                  \"token_exchange_service\": \"securetoken.googleapis.com\",\n"
            + "                  \"subject_token_path\": \"/etc/secret/sajwt.token\"\n"
            + "                }\n"
            + "              }]\n" // end call_credentials
            + "            },\n" // end google_grpc
            + "            \"time_out\": {\"seconds\": 10}\n"
            + "          }]\n" // end grpc_services
            + "        },\n" // end server
            + "        \"certificate_lifetime\": {\"seconds\": 86400},\n"
            + "        \"renewal_grace_period\": {\"seconds\": 3600},\n"
            + "        \"key_type\": \"RSA\",\n"
            + "        \"key_size\": 2048,\n"
            + "        \"location\": \"https://container.googleapis.com/v1/project/test-project1/locations/test-zone2/clusters/test-cluster3\"\n"
            + "      }\n" // end config
            + "    },\n" // end gcp_id
            + "    \"file_provider\": {\n"
            + "      \"plugin_name\": \"file_watcher\",\n"
            + "      \"config\": {\"path\": \"/etc/secret/certs\"}\n"
            + "    }\n"
            + "  }\n"
            + "}";

    bootstrapper.setFileReader(createFileReader(BOOTSTRAP_FILE_PATH, rawData));
    BootstrapInfo info = bootstrapper.bootstrap();
    assertThat(info.servers()).isEmpty();
    assertThat(info.node()).isEqualTo(getNodeBuilder().build());
    Map<String, Bootstrapper.CertificateProviderInfo> certProviders = info.certProviders();
    assertThat(certProviders).isNotNull();
    Bootstrapper.CertificateProviderInfo gcpId = certProviders.get("gcp_id");
    Bootstrapper.CertificateProviderInfo fileProvider = certProviders.get("file_provider");
    assertThat(gcpId.pluginName()).isEqualTo("meshca");
    assertThat(gcpId.config()).isInstanceOf(Map.class);
    assertThat(fileProvider.pluginName()).isEqualTo("file_watcher");
    assertThat(fileProvider.config()).isInstanceOf(Map.class);
    Map<String, ?> meshCaConfig = gcpId.config();
    assertThat(meshCaConfig.get("key_size")).isEqualTo(2048);
  }

  @Test
  public void parseBootstrap_badPluginName() throws XdsInitializationException {
    String rawData =
        "{\n"
            + "  \"xds_servers\": [],\n"
            + "  \"certificate_providers\": {\n"
            + "    \"gcp_id\": {\n"
            + "      \"plugin_name\": 234,\n"
            + "      \"config\": {\n"
            + "        \"server\": {\n"
            + "          \"api_type\": \"GRPC\",\n"
            + "          \"grpc_services\": [{\n"
            + "            \"google_grpc\": {\n"
            + "              \"target_uri\": \"meshca.com\",\n"
            + "              \"channel_credentials\": {\"google_default\": {}},\n"
            + "              \"call_credentials\": [{\n"
            + "                \"sts_service\": {\n"
            + "                  \"token_exchange_service\": \"securetoken.googleapis.com\",\n"
            + "                  \"subject_token_path\": \"/etc/secret/sajwt.token\"\n"
            + "                }\n"
            + "              }]\n" // end call_credentials
            + "            },\n" // end google_grpc
            + "            \"time_out\": {\"seconds\": 10}\n"
            + "          }]\n" // end grpc_services
            + "        },\n" // end server
            + "        \"certificate_lifetime\": {\"seconds\": 86400},\n"
            + "        \"renewal_grace_period\": {\"seconds\": 3600},\n"
            + "        \"key_type\": \"RSA\",\n"
            + "        \"key_size\": 2048,\n"
            + "        \"location\": \"https://container.googleapis.com/v1/project/test-project1/locations/test-zone2/clusters/test-cluster3\"\n"
            + "      }\n" // end config
            + "    },\n" // end gcp_id
            + "    \"file_provider\": {\n"
            + "      \"plugin_name\": \"file_watcher\",\n"
            + "      \"config\": {\"path\": \"/etc/secret/certs\"}\n"
            + "    }\n"
            + "  }\n"
            + "}";

    bootstrapper.setFileReader(createFileReader(BOOTSTRAP_FILE_PATH, rawData));
    try {
      bootstrapper.bootstrap();
      fail("exception expected");
    } catch (ClassCastException expected) {
      assertThat(expected).hasMessageThat().contains("value '234.0' for key 'plugin_name' in");
    }
  }

  @Test
  public void parseBootstrap_badConfig() throws XdsInitializationException {
    String rawData =
        "{\n"
            + "  \"xds_servers\": [],\n"
            + "  \"certificate_providers\": {\n"
            + "    \"gcp_id\": {\n"
            + "      \"plugin_name\": \"meshca\",\n"
            + "      \"config\": \"badValue\"\n"
            + "    },\n" // end gcp_id
            + "    \"file_provider\": {\n"
            + "      \"plugin_name\": \"file_watcher\",\n"
            + "      \"config\": {\"path\": \"/etc/secret/certs\"}\n"
            + "    }\n"
            + "  }\n"
            + "}";

    bootstrapper.setFileReader(createFileReader(BOOTSTRAP_FILE_PATH, rawData));
    try {
      bootstrapper.bootstrap();
      fail("exception expected");
    } catch (ClassCastException expected) {
      assertThat(expected).hasMessageThat().contains("value 'badValue' for key 'config' in");
    }
  }

  @Test
  public void parseBootstrap_missingConfig() {
    String rawData =
        "{\n"
            + "  \"xds_servers\": [],\n"
            + "  \"certificate_providers\": {\n"
            + "    \"gcp_id\": {\n"
            + "      \"plugin_name\": \"meshca\"\n"
            + "    },\n" // end gcp_id
            + "    \"file_provider\": {\n"
            + "      \"plugin_name\": \"file_watcher\",\n"
            + "      \"config\": {\"path\": \"/etc/secret/certs\"}\n"
            + "    }\n"
            + "  }\n"
            + "}";

    bootstrapper.setFileReader(createFileReader(BOOTSTRAP_FILE_PATH, rawData));
    try {
      bootstrapper.bootstrap();
      fail("exception expected");
    } catch (XdsInitializationException expected) {
      assertThat(expected)
          .hasMessageThat()
          .isEqualTo("Invalid bootstrap: 'config' does not exist.");
    }
  }

  @Test
  public void parseBootstrap_missingPluginName() {
    String rawData =
        "{\n"
            + "  \"xds_servers\": [],\n"
            + "  \"certificate_providers\": {\n"
            + "    \"gcp_id\": {\n"
            + "      \"plugin_name\": \"meshca\",\n"
            + "      \"config\": {\n"
            + "        \"server\": {\n"
            + "          \"api_type\": \"GRPC\",\n"
            + "          \"grpc_services\": [{\n"
            + "            \"google_grpc\": {\n"
            + "              \"target_uri\": \"meshca.com\",\n"
            + "              \"channel_credentials\": {\"google_default\": {}},\n"
            + "              \"call_credentials\": [{\n"
            + "                \"sts_service\": {\n"
            + "                  \"token_exchange_service\": \"securetoken.googleapis.com\",\n"
            + "                  \"subject_token_path\": \"/etc/secret/sajwt.token\"\n"
            + "                }\n"
            + "              }]\n" // end call_credentials
            + "            },\n" // end google_grpc
            + "            \"time_out\": {\"seconds\": 10}\n"
            + "          }]\n" // end grpc_services
            + "        },\n" // end server
            + "        \"certificate_lifetime\": {\"seconds\": 86400},\n"
            + "        \"renewal_grace_period\": {\"seconds\": 3600},\n"
            + "        \"key_type\": \"RSA\",\n"
            + "        \"key_size\": 2048,\n"
            + "        \"location\": \"https://container.googleapis.com/v1/project/test-project1/locations/test-zone2/clusters/test-cluster3\"\n"
            + "      }\n" // end config
            + "    },\n" // end gcp_id
            + "    \"file_provider\": {\n"
            + "      \"config\": {\"path\": \"/etc/secret/certs\"}\n"
            + "    }\n"
            + "  }\n"
            + "}";

    bootstrapper.setFileReader(createFileReader(BOOTSTRAP_FILE_PATH, rawData));
    try {
      bootstrapper.bootstrap();
      fail("exception expected");
    } catch (XdsInitializationException expected) {
      assertThat(expected)
          .hasMessageThat()
          .isEqualTo("Invalid bootstrap: 'plugin_name' does not exist.");
    }
  }

  @Test
  public void parseBootstrap_grpcServerResourceId() throws XdsInitializationException {
    String rawData = "{\n"
            + "  \"xds_servers\": [],\n"
            + "  \"server_listener_resource_name_template\": \"grpc/serverx=%s\"\n"
            + "}";

    bootstrapper.setFileReader(createFileReader(BOOTSTRAP_FILE_PATH, rawData));
    BootstrapInfo info = bootstrapper.bootstrap();
    assertThat(info.serverListenerResourceNameTemplate()).isEqualTo("grpc/serverx=%s");
  }

  @Test
  public void useV2ProtocolByDefault() throws XdsInitializationException {
    String rawData = "{\n"
        + "  \"xds_servers\": [\n"
        + "    {\n"
        + "      \"server_uri\": \"" + SERVER_URI + "\",\n"
        + "      \"channel_creds\": [\n"
        + "        {\"type\": \"insecure\"}\n"
        + "      ],\n"
        + "      \"server_features\": []\n"
        + "    }\n"
        + "  ]\n"
        + "}";

    bootstrapper.setFileReader(createFileReader(BOOTSTRAP_FILE_PATH, rawData));
    BootstrapInfo info = bootstrapper.bootstrap();
    ServerInfo serverInfo = Iterables.getOnlyElement(info.servers());
    assertThat(serverInfo.target()).isEqualTo(SERVER_URI);
    assertThat(serverInfo.implSpecificConfig()).isInstanceOf(InsecureChannelCredentials.class);
    assertThat(serverInfo.ignoreResourceDeletion()).isFalse();
  }

  @Test
  public void useV3ProtocolIfV3FeaturePresent() throws XdsInitializationException {
    String rawData = "{\n"
        + "  \"xds_servers\": [\n"
        + "    {\n"
        + "      \"server_uri\": \"" + SERVER_URI + "\",\n"
        + "      \"channel_creds\": [\n"
        + "        {\"type\": \"insecure\"}\n"
        + "      ],\n"
        + "      \"server_features\": [\"xds_v3\"]\n"
        + "    }\n"
        + "  ]\n"
        + "}";

    bootstrapper.setFileReader(createFileReader(BOOTSTRAP_FILE_PATH, rawData));
    BootstrapInfo info = bootstrapper.bootstrap();
    ServerInfo serverInfo = Iterables.getOnlyElement(info.servers());
    assertThat(serverInfo.target()).isEqualTo(SERVER_URI);
    assertThat(serverInfo.implSpecificConfig()).isInstanceOf(InsecureChannelCredentials.class);
    assertThat(serverInfo.ignoreResourceDeletion()).isFalse();
  }

  @Test
  public void serverFeatureIgnoreResourceDeletion() throws XdsInitializationException {
    String rawData = "{\n"
        + "  \"xds_servers\": [\n"
        + "    {\n"
        + "      \"server_uri\": \"" + SERVER_URI + "\",\n"
        + "      \"channel_creds\": [\n"
        + "        {\"type\": \"insecure\"}\n"
        + "      ],\n"
        + "      \"server_features\": [\"ignore_resource_deletion\"]\n"
        + "    }\n"
        + "  ]\n"
        + "}";

    bootstrapper.setFileReader(createFileReader(BOOTSTRAP_FILE_PATH, rawData));
    BootstrapInfo info = bootstrapper.bootstrap();
    ServerInfo serverInfo = Iterables.getOnlyElement(info.servers());
    assertThat(serverInfo.target()).isEqualTo(SERVER_URI);
    assertThat(serverInfo.implSpecificConfig()).isInstanceOf(InsecureChannelCredentials.class);
    // Only ignore_resource_deletion feature enabled: confirm it's on, and xds_v3 is off.
    assertThat(serverInfo.ignoreResourceDeletion()).isTrue();
  }

  @Test
  public void serverFeatureIgnoreResourceDeletion_xdsV3() throws XdsInitializationException {
    String rawData = "{\n"
        + "  \"xds_servers\": [\n"
        + "    {\n"
        + "      \"server_uri\": \"" + SERVER_URI + "\",\n"
        + "      \"channel_creds\": [\n"
        + "        {\"type\": \"insecure\"}\n"
        + "      ],\n"
        + "      \"server_features\": [\"xds_v3\", \"ignore_resource_deletion\"]\n"
        + "    }\n"
        + "  ]\n"
        + "}";

    bootstrapper.setFileReader(createFileReader(BOOTSTRAP_FILE_PATH, rawData));
    BootstrapInfo info = bootstrapper.bootstrap();
    ServerInfo serverInfo = Iterables.getOnlyElement(info.servers());
    assertThat(serverInfo.target()).isEqualTo(SERVER_URI);
    assertThat(serverInfo.implSpecificConfig()).isInstanceOf(InsecureChannelCredentials.class);
    // ignore_resource_deletion features enabled: confirm both are on.
    assertThat(serverInfo.ignoreResourceDeletion()).isTrue();
  }

  @Test
  public void notFound() {
    bootstrapper.bootstrapPathFromEnvVar = null;
    bootstrapper.bootstrapPathFromSysProp = null;
    bootstrapper.bootstrapConfigFromEnvVar = null;
    bootstrapper.bootstrapConfigFromSysProp = null;
    BootstrapperImpl.FileReader reader = mock(BootstrapperImpl.FileReader.class);
    bootstrapper.setFileReader(reader);
    try {
      bootstrapper.bootstrap();
      fail("should fail");
    } catch (XdsInitializationException expected) {
      assertThat(expected).hasMessageThat().startsWith("Cannot find bootstrap configuration");
    }
    verifyNoInteractions(reader);
  }

  @Test
  public void fallbackToFilePathFromSystemProperty() throws XdsInitializationException {
    final String customPath = "/home/bootstrap.json";
    bootstrapper.bootstrapPathFromEnvVar = null;
    bootstrapper.bootstrapPathFromSysProp = customPath;
    String rawData = "{\n"
        + "  \"xds_servers\": [\n"
        + "    {\n"
        + "      \"server_uri\": \"" + SERVER_URI + "\",\n"
        + "      \"channel_creds\": [\n"
        + "        {\"type\": \"insecure\"}\n"
        + "      ]\n"
        + "    }\n"
        + "  ]\n"
        + "}";

    bootstrapper.setFileReader(createFileReader(customPath, rawData));
    bootstrapper.bootstrap();
  }

  @Test
  public void fallbackToConfigFromEnvVar() throws XdsInitializationException {
    String rawData = "{\n"
        + "  \"xds_servers\": [\n"
        + "    {\n"
        + "      \"server_uri\": \"" + SERVER_URI + "\",\n"
        + "      \"channel_creds\": [\n"
        + "        {\"type\": \"insecure\"}\n"
        + "      ]\n"
        + "    }\n"
        + "  ]\n"
        + "}";

    bootstrapper.bootstrapPathFromEnvVar = null;
    bootstrapper.bootstrapPathFromSysProp = null;
    bootstrapper.bootstrapConfigFromEnvVar = rawData;
    bootstrapper.setFileReader(mock(BootstrapperImpl.FileReader.class));
    bootstrapper.bootstrap();
  }

  @Test
  public void fallbackToConfigFromSysProp() throws XdsInitializationException {
    String rawData = "{\n"
        + "  \"xds_servers\": [\n"
        + "    {\n"
        + "      \"server_uri\": \"" + SERVER_URI + "\",\n"
        + "      \"channel_creds\": [\n"
        + "        {\"type\": \"insecure\"}\n"
        + "      ]\n"
        + "    }\n"
        + "  ]\n"
        + "}";

    bootstrapper.bootstrapPathFromEnvVar = null;
    bootstrapper.bootstrapPathFromSysProp = null;
    bootstrapper.bootstrapConfigFromEnvVar = null;
    bootstrapper.bootstrapConfigFromSysProp = rawData;
    bootstrapper.setFileReader(mock(BootstrapperImpl.FileReader.class));
    bootstrapper.bootstrap();
  }

  @Test
  public void parseClientDefaultListenerResourceNameTemplate() throws Exception {
    String rawData = "{\n"
        + "  \"xds_servers\": [\n"
        + "  ]\n"
        + "}";
    bootstrapper.setFileReader(createFileReader(BOOTSTRAP_FILE_PATH, rawData));
    BootstrapInfo info = bootstrapper.bootstrap();
    assertThat(info.clientDefaultListenerResourceNameTemplate()).isEqualTo("%s");

    rawData = "{\n"
        + "  \"client_default_listener_resource_name_template\": \"xdstp://a.com/faketype/%s\",\n"
        + "  \"xds_servers\": [\n"
        + "  ]\n"
        + "}";
    bootstrapper.setFileReader(createFileReader(BOOTSTRAP_FILE_PATH, rawData));
    info = bootstrapper.bootstrap();
    assertThat(info.clientDefaultListenerResourceNameTemplate())
        .isEqualTo("xdstp://a.com/faketype/%s");
  }

  @Test
  public void parseAuthorities() throws Exception {
    String rawData = "{\n"
        + "  \"xds_servers\": [\n"
        + "    {\n"
        + "      \"server_uri\": \"" + SERVER_URI + "\",\n"
        + "      \"channel_creds\": [\n"
        + "        {\"type\": \"insecure\"}\n"
        + "      ]\n"
        + "    }\n"
        + "  ]\n"
        + "}";
    bootstrapper.setFileReader(createFileReader(BOOTSTRAP_FILE_PATH, rawData));
    BootstrapInfo info = bootstrapper.bootstrap();
    assertThat(info.authorities()).isEmpty();

    rawData = "{\n"
        + "  \"authorities\": {\n"
        + "    \"a.com\": {\n"
        + "      \"client_listener_resource_name_template\": \"xdstp://a.com/v1.Listener/id-%s\"\n"
        + "    }\n"
        + "  },\n"
        + "  \"xds_servers\": [\n"
        + "    {\n"
        + "      \"server_uri\": \"" + SERVER_URI + "\",\n"
        + "      \"channel_creds\": [\n"
        + "        {\"type\": \"insecure\"}\n"
        + "      ]\n"
        + "    }\n"
        + "  ]\n"
        + "}";
    bootstrapper.setFileReader(createFileReader(BOOTSTRAP_FILE_PATH, rawData));
    info = bootstrapper.bootstrap();
    assertThat(info.authorities()).hasSize(1);
    AuthorityInfo authorityInfo = info.authorities().get("a.com");
    assertThat(authorityInfo.clientListenerResourceNameTemplate())
        .isEqualTo("xdstp://a.com/v1.Listener/id-%s");
    // Defaults to top-level servers.
    assertThat(authorityInfo.xdsServers()).hasSize(1);
    assertThat(authorityInfo.xdsServers().get(0).target()).isEqualTo(SERVER_URI);

    rawData = "{\n"
        + "  \"authorities\": {\n"
        + "    \"a.com\": {\n"
        + "      \"xds_servers\": [\n"
        + "        {\n"
        + "          \"server_uri\": \"td2.googleapis.com:443\",\n"
        + "          \"channel_creds\": [\n"
        + "            {\"type\": \"insecure\"}\n"
        + "          ]\n"
        + "        }\n"
        + "      ]\n"
        + "    }\n"
        + "  },\n"
        + "  \"xds_servers\": [\n"
        + "    {\n"
        + "      \"server_uri\": \"" + SERVER_URI + "\",\n"
        + "      \"channel_creds\": [\n"
        + "        {\"type\": \"insecure\"}\n"
        + "      ]\n"
        + "    }\n"
        + "  ]\n"
        + "}";
    bootstrapper.setFileReader(createFileReader(BOOTSTRAP_FILE_PATH, rawData));
    info = bootstrapper.bootstrap();
    assertThat(info.authorities()).hasSize(1);
    authorityInfo = info.authorities().get("a.com");
    // Defaults to "xdstp://<authority_name>>/envoy.config.listener.v3.Listener/%s"
    assertThat(authorityInfo.clientListenerResourceNameTemplate())
        .isEqualTo("xdstp://a.com/envoy.config.listener.v3.Listener/%s");
    assertThat(authorityInfo.xdsServers()).hasSize(1);
    assertThat(authorityInfo.xdsServers().get(0).target()).isEqualTo("td2.googleapis.com:443");
  }

  @Test
  public void badFederationConfig() throws Exception {
    String rawData = "{\n"
        + "  \"authorities\": {\n"
        + "    \"a.com\": {\n"
        + "      \"client_listener_resource_name_template\": \"xdstp://wrong/\"\n"
        + "    }\n"
        + "  },\n"
        + "  \"xds_servers\": [\n"
        + "    {\n"
        + "      \"server_uri\": \"" + SERVER_URI + "\",\n"
        + "      \"channel_creds\": [\n"
        + "        {\"type\": \"insecure\"}\n"
        + "      ]\n"
        + "    }\n"
        + "  ]\n"
        + "}";
    bootstrapper.setFileReader(createFileReader(BOOTSTRAP_FILE_PATH, rawData));
    try {
      bootstrapper.bootstrap();
      fail("should fail");
    } catch (XdsInitializationException e) {
      assertThat(e).hasMessageThat().isEqualTo(
          "client_listener_resource_name_template: 'xdstp://wrong/' does not start with "
              + "xdstp://a.com/");
    }
  }

  private static BootstrapperImpl.FileReader createFileReader(
      final String expectedPath, final String rawData) {
    return new BootstrapperImpl.FileReader() {
      @Override
      public String readFile(String path) throws IOException {
        assertThat(path).isEqualTo(expectedPath);
        return rawData;
      }
    };
  }

  private static Node.Builder getNodeBuilder() {
    GrpcBuildVersion buildVersion = GrpcUtil.getGrpcBuildVersion();
    return
        Node.newBuilder()
            .setBuildVersion(buildVersion.toString())
            .setUserAgentName(buildVersion.getUserAgent())
            .setUserAgentVersion(buildVersion.getImplementationVersion())
            .addClientFeatures(DubboBootstrapperImpl.CLIENT_FEATURE_DISABLE_OVERPROVISIONING)
            .addClientFeatures(DubboBootstrapperImpl.CLIENT_FEATURE_RESOURCE_IN_SOTW);
  }
}
