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

package org.apache.dubbo.xds.resource.grpc;

import org.apache.dubbo.xds.XdsInitializationException;
import org.apache.dubbo.xds.resource.grpc.EnvoyProtoData.Node;

import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.grpc.ChannelCredentials;
import io.grpc.Internal;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Loads configuration information to bootstrap gRPC's integration of xDS protocol.
 */
@Internal
public abstract class Bootstrapper {

  static final String XDSTP_SCHEME = "xdstp:";

  /**
   * Returns system-loaded bootstrap configuration.
   */
  public abstract BootstrapInfo bootstrap() throws XdsInitializationException;

  /**
   * Returns bootstrap configuration given by the raw data in JSON format.
   */
  BootstrapInfo bootstrap(Map<String, ?> rawData) throws XdsInitializationException {
    throw new UnsupportedOperationException();
  }

  /**
   * Data class containing xDS server information, such as server URI and channel credentials
   * to be used for communication.
   */
  @AutoValue
  @Internal
  abstract static class ServerInfo {
    abstract String target();

    abstract ChannelCredentials channelCredentials();

    abstract boolean ignoreResourceDeletion();

    @VisibleForTesting
    static ServerInfo create(
        String target, ChannelCredentials channelCredentials) {
      return new AutoValue_Bootstrapper_ServerInfo(target, channelCredentials, false);
    }

    @VisibleForTesting
    static ServerInfo create(
        String target, ChannelCredentials channelCredentials,
        boolean ignoreResourceDeletion) {
      return new AutoValue_Bootstrapper_ServerInfo(target, channelCredentials,
          ignoreResourceDeletion);
    }
  }

  /**
   * Data class containing Certificate provider information: the plugin-name and an opaque
   * Map that represents the config for that plugin.
   */
  @AutoValue
  @Internal
  public abstract static class CertificateProviderInfo {
    public abstract String pluginName();

    public abstract ImmutableMap<String, ?> config();

    @VisibleForTesting
    public static CertificateProviderInfo create(String pluginName, Map<String, ?> config) {
      return new AutoValue_Bootstrapper_CertificateProviderInfo(
          pluginName, ImmutableMap.copyOf(config));
    }
  }

  @AutoValue
  abstract static class AuthorityInfo {

    /**
     * A template for the name of the Listener resource to subscribe to for a gRPC client
     * channel. Used only when the channel is created using an "xds:" URI with this authority
     * name.
     *
     * <p>The token "%s", if present in this string, will be replaced with %-encoded
     * service authority (i.e., the path part of the target URI used to create the gRPC channel).
     *
     * <p>Return value must start with {@code "xdstp://<authority_name>/"}.
     */
    abstract String clientListenerResourceNameTemplate();

    /**
     * Ordered list of xDS servers to contact for this authority.
     *
     * <p>If the same server is listed in multiple authorities, the entries will be de-duped (i.e.,
     * resources for both authorities will be fetched on the same ADS stream).
     *
     * <p>Defaults to the top-level server list {@link BootstrapInfo#servers()}. Must not be empty.
     */
    abstract ImmutableList<ServerInfo> xdsServers();

    static AuthorityInfo create(
        String clientListenerResourceNameTemplate, List<ServerInfo> xdsServers) {
      checkArgument(!xdsServers.isEmpty(), "xdsServers must not be empty");
      return new AutoValue_Bootstrapper_AuthorityInfo(
          clientListenerResourceNameTemplate, ImmutableList.copyOf(xdsServers));
    }
  }

  /**
   * Data class containing the results of reading bootstrap.
   */
  @AutoValue
  @Internal
  public abstract static class BootstrapInfo {
    /** Returns the list of xDS servers to be connected to. Must not be empty. */
    abstract ImmutableList<ServerInfo> servers();

    /** Returns the node identifier to be included in xDS requests. */
    public abstract Node node();

    /** Returns the cert-providers config map. */
    @Nullable
    public abstract ImmutableMap<String, CertificateProviderInfo> certProviders();

    /**
     * A template for the name of the Listener resource to subscribe to for a gRPC server.
     *
     * <p>If starts with "xdstp:", will be interpreted as a new-style name, in which case the
     * authority of the URI will be used to select the relevant configuration in the
     * "authorities" map. The token "%s", if present in this string, will be replaced with
     * the IP and port on which the server is listening. If the template starts with "xdstp:",
     * the replaced string will be %-encoded.
     *
     * <p>There is no default; if unset, xDS-based server creation fails.
     */
    @Nullable
    public abstract String serverListenerResourceNameTemplate();

    /**
     * A template for the name of the Listener resource to subscribe to for a gRPC client channel.
     * Used only when the channel is created with an "xds:" URI with no authority.
     *
     * <p>If starts with "xdstp:", will be interpreted as a new-style name, in which case the
     * authority of the URI will be used to select the relevant configuration in the "authorities"
     * map.
     *
     * <p>The token "%s", if present in this string, will be replaced with the service authority
     * (i.e., the path part of the target URI used to create the gRPC channel). If the template
     * starts with "xdstp:", the replaced string will be %-encoded.
     *
     * <p>Defaults to {@code "%s"}.
     */
    abstract String clientDefaultListenerResourceNameTemplate();

    /**
     * A map of authority name to corresponding configuration.
     *
     * <p>This is used in the following cases:
     *
     * <ul>
     * <li>A gRPC client channel is created using an "xds:" URI that includes  an
     * authority.</li>
     *
     * <li>A gRPC client channel is created using an "xds:" URI with no authority,
     * but the "client_default_listener_resource_name_template" field above turns it into an
     * "xdstp:" URI.</li>
     *
     * <li>A gRPC server is created and the "server_listener_resource_name_template" field is an
     * "xdstp:" URI.</li>
     * </ul>
     *
     * <p>In any of those cases, it is an error if the specified authority is not present in this
     * map.
     *
     * <p>Defaults to an empty map.
     */
    abstract ImmutableMap<String, AuthorityInfo> authorities();

    @VisibleForTesting
    static Builder builder() {
      return new AutoValue_Bootstrapper_BootstrapInfo.Builder()
          .clientDefaultListenerResourceNameTemplate("%s")
          .authorities(ImmutableMap.<String, AuthorityInfo>of());
    }

    @AutoValue.Builder
    @VisibleForTesting
    abstract static class Builder {

      abstract Builder servers(List<ServerInfo> servers);

      abstract Builder node(Node node);

      abstract Builder certProviders(@Nullable Map<String, CertificateProviderInfo> certProviders);

      abstract Builder serverListenerResourceNameTemplate(
          @Nullable String serverListenerResourceNameTemplate);

      abstract Builder clientDefaultListenerResourceNameTemplate(
          String clientDefaultListenerResourceNameTemplate);

      abstract Builder authorities(Map<String, AuthorityInfo> authorities);

      abstract BootstrapInfo build();
    }
  }
}
