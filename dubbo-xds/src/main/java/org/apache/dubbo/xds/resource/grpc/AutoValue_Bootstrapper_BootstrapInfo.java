package org.apache.dubbo.xds.resource.grpc;

import org.apache.dubbo.common.lang.Nullable;
import org.apache.dubbo.xds.resource.grpc.Bootstrapper.CertificateProviderInfo;
import org.apache.dubbo.xds.resource.grpc.Bootstrapper.ServerInfo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;

final class AutoValue_Bootstrapper_BootstrapInfo extends Bootstrapper.BootstrapInfo {

  private final ImmutableList<ServerInfo> servers;

  private final EnvoyProtoData.Node node;

  @Nullable
  private final ImmutableMap<String, CertificateProviderInfo> certProviders;

  @Nullable
  private final String serverListenerResourceNameTemplate;

  private final String clientDefaultListenerResourceNameTemplate;

  private final ImmutableMap<String, Bootstrapper.AuthorityInfo> authorities;

  private AutoValue_Bootstrapper_BootstrapInfo(
      ImmutableList<Bootstrapper.ServerInfo> servers,
      EnvoyProtoData.Node node,
      @Nullable ImmutableMap<String, Bootstrapper.CertificateProviderInfo> certProviders,
      @Nullable String serverListenerResourceNameTemplate,
      String clientDefaultListenerResourceNameTemplate,
      ImmutableMap<String, Bootstrapper.AuthorityInfo> authorities) {
    this.servers = servers;
    this.node = node;
    this.certProviders = certProviders;
    this.serverListenerResourceNameTemplate = serverListenerResourceNameTemplate;
    this.clientDefaultListenerResourceNameTemplate = clientDefaultListenerResourceNameTemplate;
    this.authorities = authorities;
  }

  @Override
  ImmutableList<Bootstrapper.ServerInfo> servers() {
    return servers;
  }

  @Override
  public EnvoyProtoData.Node node() {
    return node;
  }

  @Nullable
  @Override
  public ImmutableMap<String, Bootstrapper.CertificateProviderInfo> certProviders() {
    return certProviders;
  }

  @Nullable
  @Override
  public String serverListenerResourceNameTemplate() {
    return serverListenerResourceNameTemplate;
  }

  @Override
  String clientDefaultListenerResourceNameTemplate() {
    return clientDefaultListenerResourceNameTemplate;
  }

  @Override
  ImmutableMap<String, Bootstrapper.AuthorityInfo> authorities() {
    return authorities;
  }

  @Override
  public String toString() {
    return "BootstrapInfo{"
        + "servers=" + servers + ", "
        + "node=" + node + ", "
        + "certProviders=" + certProviders + ", "
        + "serverListenerResourceNameTemplate=" + serverListenerResourceNameTemplate + ", "
        + "clientDefaultListenerResourceNameTemplate=" + clientDefaultListenerResourceNameTemplate + ", "
        + "authorities=" + authorities
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof Bootstrapper.BootstrapInfo) {
      Bootstrapper.BootstrapInfo that = (Bootstrapper.BootstrapInfo) o;
      return this.servers.equals(that.servers())
          && this.node.equals(that.node())
          && (this.certProviders == null ? that.certProviders() == null : this.certProviders.equals(that.certProviders()))
          && (this.serverListenerResourceNameTemplate == null ? that.serverListenerResourceNameTemplate() == null : this.serverListenerResourceNameTemplate.equals(that.serverListenerResourceNameTemplate()))
          && this.clientDefaultListenerResourceNameTemplate.equals(that.clientDefaultListenerResourceNameTemplate())
          && this.authorities.equals(that.authorities());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= servers.hashCode();
    h$ *= 1000003;
    h$ ^= node.hashCode();
    h$ *= 1000003;
    h$ ^= (certProviders == null) ? 0 : certProviders.hashCode();
    h$ *= 1000003;
    h$ ^= (serverListenerResourceNameTemplate == null) ? 0 : serverListenerResourceNameTemplate.hashCode();
    h$ *= 1000003;
    h$ ^= clientDefaultListenerResourceNameTemplate.hashCode();
    h$ *= 1000003;
    h$ ^= authorities.hashCode();
    return h$;
  }

  static final class Builder extends Bootstrapper.BootstrapInfo.Builder {
    private ImmutableList<Bootstrapper.ServerInfo> servers;
    private EnvoyProtoData.Node node;
    private ImmutableMap<String, Bootstrapper.CertificateProviderInfo> certProviders;
    private String serverListenerResourceNameTemplate;
    private String clientDefaultListenerResourceNameTemplate;
    private ImmutableMap<String, Bootstrapper.AuthorityInfo> authorities;
    Builder() {
    }
    @Override
    Bootstrapper.BootstrapInfo.Builder servers(List<ServerInfo> servers) {
      this.servers = ImmutableList.copyOf(servers);
      return this;
    }
    @Override
    Bootstrapper.BootstrapInfo.Builder node(EnvoyProtoData.Node node) {
      if (node == null) {
        throw new NullPointerException("Null node");
      }
      this.node = node;
      return this;
    }
    @Override
    Bootstrapper.BootstrapInfo.Builder certProviders(@Nullable Map<String, CertificateProviderInfo> certProviders) {
      this.certProviders = (certProviders == null ? null : ImmutableMap.copyOf(certProviders));
      return this;
    }
    @Override
    Bootstrapper.BootstrapInfo.Builder serverListenerResourceNameTemplate(@Nullable String serverListenerResourceNameTemplate) {
      this.serverListenerResourceNameTemplate = serverListenerResourceNameTemplate;
      return this;
    }
    @Override
    Bootstrapper.BootstrapInfo.Builder clientDefaultListenerResourceNameTemplate(String clientDefaultListenerResourceNameTemplate) {
      if (clientDefaultListenerResourceNameTemplate == null) {
        throw new NullPointerException("Null clientDefaultListenerResourceNameTemplate");
      }
      this.clientDefaultListenerResourceNameTemplate = clientDefaultListenerResourceNameTemplate;
      return this;
    }
    @Override
    Bootstrapper.BootstrapInfo.Builder authorities(Map<String, Bootstrapper.AuthorityInfo> authorities) {
      this.authorities = ImmutableMap.copyOf(authorities);
      return this;
    }
    @Override
    Bootstrapper.BootstrapInfo build() {
      if (this.servers == null
          || this.node == null
          || this.clientDefaultListenerResourceNameTemplate == null
          || this.authorities == null) {
        StringBuilder missing = new StringBuilder();
        if (this.servers == null) {
          missing.append(" servers");
        }
        if (this.node == null) {
          missing.append(" node");
        }
        if (this.clientDefaultListenerResourceNameTemplate == null) {
          missing.append(" clientDefaultListenerResourceNameTemplate");
        }
        if (this.authorities == null) {
          missing.append(" authorities");
        }
        throw new IllegalStateException("Missing required properties:" + missing);
      }
      return new AutoValue_Bootstrapper_BootstrapInfo(
          this.servers,
          this.node,
          this.certProviders,
          this.serverListenerResourceNameTemplate,
          this.clientDefaultListenerResourceNameTemplate,
          this.authorities);
    }
  }

}
