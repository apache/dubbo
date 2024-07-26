package org.apache.dubbo.xds.resource.grpc.resource.clusterPlugin;

public class NamedPluginConfig {

    private final String name;

    private final PluginConfig config;

    NamedPluginConfig(
            String name, PluginConfig config) {
        if (name == null) {
            throw new NullPointerException("Null name");
        }
        this.name = name;
        if (config == null) {
            throw new NullPointerException("Null config");
        }
        this.config = config;
    }

    String name() {
        return name;
    }

    PluginConfig config() {
        return config;
    }

    @Override
    public String toString() {
        return "NamedPluginConfig{" + "name=" + name + ", " + "config=" + config + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof NamedPluginConfig) {
            NamedPluginConfig that = (NamedPluginConfig) o;
            return this.name.equals(that.name()) && this.config.equals(that.config());
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

    public static NamedPluginConfig create(String name, PluginConfig config) {
        return new NamedPluginConfig(name, config);
    }
}
