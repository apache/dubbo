package org.apache.dubbo.xds.resource.grpc;

import org.apache.dubbo.common.lang.Nullable;

import com.google.common.collect.ImmutableList;

final class AutoValue_HttpConnectionManager extends HttpConnectionManager {

  private final long httpMaxStreamDurationNano;

  @Nullable
  private final String rdsName;

  @Nullable
  private final ImmutableList<VirtualHost> virtualHosts;

  @Nullable
  private final ImmutableList<Filter.NamedFilterConfig> httpFilterConfigs;

  AutoValue_HttpConnectionManager(
      long httpMaxStreamDurationNano,
      @Nullable String rdsName,
      @Nullable ImmutableList<VirtualHost> virtualHosts,
      @Nullable ImmutableList<Filter.NamedFilterConfig> httpFilterConfigs) {
    this.httpMaxStreamDurationNano = httpMaxStreamDurationNano;
    this.rdsName = rdsName;
    this.virtualHosts = virtualHosts;
    this.httpFilterConfigs = httpFilterConfigs;
  }

  @Override
  long httpMaxStreamDurationNano() {
    return httpMaxStreamDurationNano;
  }

  @Nullable
  @Override
  String rdsName() {
    return rdsName;
  }

  @Nullable
  @Override
  ImmutableList<VirtualHost> virtualHosts() {
    return virtualHosts;
  }

  @Nullable
  @Override
  ImmutableList<Filter.NamedFilterConfig> httpFilterConfigs() {
    return httpFilterConfigs;
  }

  @Override
  public String toString() {
    return "HttpConnectionManager{"
        + "httpMaxStreamDurationNano=" + httpMaxStreamDurationNano + ", "
        + "rdsName=" + rdsName + ", "
        + "virtualHosts=" + virtualHosts + ", "
        + "httpFilterConfigs=" + httpFilterConfigs
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof HttpConnectionManager) {
      HttpConnectionManager that = (HttpConnectionManager) o;
      return this.httpMaxStreamDurationNano == that.httpMaxStreamDurationNano()
          && (this.rdsName == null ? that.rdsName() == null : this.rdsName.equals(that.rdsName()))
          && (this.virtualHosts == null ? that.virtualHosts() == null : this.virtualHosts.equals(that.virtualHosts()))
          && (this.httpFilterConfigs == null ? that.httpFilterConfigs() == null : this.httpFilterConfigs.equals(that.httpFilterConfigs()));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= (int) ((httpMaxStreamDurationNano >>> 32) ^ httpMaxStreamDurationNano);
    h$ *= 1000003;
    h$ ^= (rdsName == null) ? 0 : rdsName.hashCode();
    h$ *= 1000003;
    h$ ^= (virtualHosts == null) ? 0 : virtualHosts.hashCode();
    h$ *= 1000003;
    h$ ^= (httpFilterConfigs == null) ? 0 : httpFilterConfigs.hashCode();
    return h$;
  }

}
