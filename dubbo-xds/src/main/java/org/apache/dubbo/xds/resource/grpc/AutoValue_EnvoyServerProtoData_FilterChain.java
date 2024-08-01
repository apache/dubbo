package org.apache.dubbo.xds.resource.grpc;

import org.apache.dubbo.common.lang.Nullable;

final class AutoValue_EnvoyServerProtoData_FilterChain extends EnvoyServerProtoData.FilterChain {

  private final String name;

  private final EnvoyServerProtoData.FilterChainMatch filterChainMatch;

  private final HttpConnectionManager httpConnectionManager;

  @Nullable
  private final SslContextProviderSupplier sslContextProviderSupplier;

  AutoValue_EnvoyServerProtoData_FilterChain(
      String name,
      EnvoyServerProtoData.FilterChainMatch filterChainMatch,
      HttpConnectionManager httpConnectionManager,
      @Nullable SslContextProviderSupplier sslContextProviderSupplier) {
    if (name == null) {
      throw new NullPointerException("Null name");
    }
    this.name = name;
    if (filterChainMatch == null) {
      throw new NullPointerException("Null filterChainMatch");
    }
    this.filterChainMatch = filterChainMatch;
    if (httpConnectionManager == null) {
      throw new NullPointerException("Null httpConnectionManager");
    }
    this.httpConnectionManager = httpConnectionManager;
    this.sslContextProviderSupplier = sslContextProviderSupplier;
  }

  @Override
  String name() {
    return name;
  }

  @Override
  EnvoyServerProtoData.FilterChainMatch filterChainMatch() {
    return filterChainMatch;
  }

  @Override
  HttpConnectionManager httpConnectionManager() {
    return httpConnectionManager;
  }

  @Nullable
  @Override
  SslContextProviderSupplier sslContextProviderSupplier() {
    return sslContextProviderSupplier;
  }

  @Override
  public String toString() {
    return "FilterChain{"
        + "name=" + name + ", "
        + "filterChainMatch=" + filterChainMatch + ", "
        + "httpConnectionManager=" + httpConnectionManager + ", "
        + "sslContextProviderSupplier=" + sslContextProviderSupplier
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof EnvoyServerProtoData.FilterChain) {
      EnvoyServerProtoData.FilterChain that = (EnvoyServerProtoData.FilterChain) o;
      return this.name.equals(that.name())
          && this.filterChainMatch.equals(that.filterChainMatch())
          && this.httpConnectionManager.equals(that.httpConnectionManager())
          && (this.sslContextProviderSupplier == null ? that.sslContextProviderSupplier() == null : this.sslContextProviderSupplier.equals(that.sslContextProviderSupplier()));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= name.hashCode();
    h$ *= 1000003;
    h$ ^= filterChainMatch.hashCode();
    h$ *= 1000003;
    h$ ^= httpConnectionManager.hashCode();
    h$ *= 1000003;
    h$ ^= (sslContextProviderSupplier == null) ? 0 : sslContextProviderSupplier.hashCode();
    return h$;
  }

}
