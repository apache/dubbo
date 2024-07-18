package org.apache.dubbo.xds.resource.grpc;

import com.google.common.collect.ImmutableMap;

final class AutoValue_Bootstrapper_CertificateProviderInfo extends Bootstrapper.CertificateProviderInfo {

  private final String pluginName;

  private final ImmutableMap<String, ?> config;

  AutoValue_Bootstrapper_CertificateProviderInfo(
      String pluginName,
      ImmutableMap<String, ?> config) {
    if (pluginName == null) {
      throw new NullPointerException("Null pluginName");
    }
    this.pluginName = pluginName;
    if (config == null) {
      throw new NullPointerException("Null config");
    }
    this.config = config;
  }

  @Override
  public String pluginName() {
    return pluginName;
  }

  @Override
  public ImmutableMap<String, ?> config() {
    return config;
  }

  @Override
  public String toString() {
    return "CertificateProviderInfo{"
        + "pluginName=" + pluginName + ", "
        + "config=" + config
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof Bootstrapper.CertificateProviderInfo) {
      Bootstrapper.CertificateProviderInfo that = (Bootstrapper.CertificateProviderInfo) o;
      return this.pluginName.equals(that.pluginName())
          && this.config.equals(that.config());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= pluginName.hashCode();
    h$ *= 1000003;
    h$ ^= config.hashCode();
    return h$;
  }

}
