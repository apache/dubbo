package org.apache.dubbo.xds.resource.grpc;

import com.google.common.collect.ImmutableMap;

final class AutoValue_RouteLookupServiceClusterSpecifierPlugin_RlsPluginConfig extends RouteLookupServiceClusterSpecifierPlugin.RlsPluginConfig {

  private final ImmutableMap<String, ?> config;

  AutoValue_RouteLookupServiceClusterSpecifierPlugin_RlsPluginConfig(
      ImmutableMap<String, ?> config) {
    if (config == null) {
      throw new NullPointerException("Null config");
    }
    this.config = config;
  }

  @Override
  ImmutableMap<String, ?> config() {
    return config;
  }

  @Override
  public String toString() {
    return "RlsPluginConfig{"
        + "config=" + config
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof RouteLookupServiceClusterSpecifierPlugin.RlsPluginConfig) {
      RouteLookupServiceClusterSpecifierPlugin.RlsPluginConfig that = (RouteLookupServiceClusterSpecifierPlugin.RlsPluginConfig) o;
      return this.config.equals(that.config());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= config.hashCode();
    return h$;
  }

}
