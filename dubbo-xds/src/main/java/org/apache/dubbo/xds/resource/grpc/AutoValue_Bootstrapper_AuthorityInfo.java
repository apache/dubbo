package org.apache.dubbo.xds.resource.grpc;

import org.apache.dubbo.xds.resource.grpc.Bootstrapper.ServerInfo;

import com.google.common.collect.ImmutableList;

final class AutoValue_Bootstrapper_AuthorityInfo extends Bootstrapper.AuthorityInfo {

  private final String clientListenerResourceNameTemplate;

  private final ImmutableList<ServerInfo> xdsServers;

  AutoValue_Bootstrapper_AuthorityInfo(
      String clientListenerResourceNameTemplate,
      ImmutableList<Bootstrapper.ServerInfo> xdsServers) {
    if (clientListenerResourceNameTemplate == null) {
      throw new NullPointerException("Null clientListenerResourceNameTemplate");
    }
    this.clientListenerResourceNameTemplate = clientListenerResourceNameTemplate;
    if (xdsServers == null) {
      throw new NullPointerException("Null xdsServers");
    }
    this.xdsServers = xdsServers;
  }

  @Override
  String clientListenerResourceNameTemplate() {
    return clientListenerResourceNameTemplate;
  }

  @Override
  ImmutableList<Bootstrapper.ServerInfo> xdsServers() {
    return xdsServers;
  }

  @Override
  public String toString() {
    return "AuthorityInfo{"
        + "clientListenerResourceNameTemplate=" + clientListenerResourceNameTemplate + ", "
        + "xdsServers=" + xdsServers
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof Bootstrapper.AuthorityInfo) {
      Bootstrapper.AuthorityInfo that = (Bootstrapper.AuthorityInfo) o;
      return this.clientListenerResourceNameTemplate.equals(that.clientListenerResourceNameTemplate())
          && this.xdsServers.equals(that.xdsServers());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= clientListenerResourceNameTemplate.hashCode();
    h$ *= 1000003;
    h$ ^= xdsServers.hashCode();
    return h$;
  }

}
