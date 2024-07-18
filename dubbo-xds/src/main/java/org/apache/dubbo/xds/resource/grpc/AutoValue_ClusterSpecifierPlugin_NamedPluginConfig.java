package org.apache.dubbo.xds.resource.grpc;

final class AutoValue_ClusterSpecifierPlugin_NamedPluginConfig extends ClusterSpecifierPlugin.NamedPluginConfig {

  private final String name;

  private final ClusterSpecifierPlugin.PluginConfig config;

  AutoValue_ClusterSpecifierPlugin_NamedPluginConfig(
      String name,
      ClusterSpecifierPlugin.PluginConfig config) {
    if (name == null) {
      throw new NullPointerException("Null name");
    }
    this.name = name;
    if (config == null) {
      throw new NullPointerException("Null config");
    }
    this.config = config;
  }

  @Override
  String name() {
    return name;
  }

  @Override
  ClusterSpecifierPlugin.PluginConfig config() {
    return config;
  }

  @Override
  public String toString() {
    return "NamedPluginConfig{"
        + "name=" + name + ", "
        + "config=" + config
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof ClusterSpecifierPlugin.NamedPluginConfig) {
      ClusterSpecifierPlugin.NamedPluginConfig that = (ClusterSpecifierPlugin.NamedPluginConfig) o;
      return this.name.equals(that.name())
          && this.config.equals(that.config());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= name.hashCode();
    h$ *= 1000003;
    h$ ^= config.hashCode();
    return h$;
  }

}
