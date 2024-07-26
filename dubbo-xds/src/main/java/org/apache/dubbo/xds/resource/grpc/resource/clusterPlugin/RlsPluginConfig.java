package org.apache.dubbo.xds.resource.grpc.resource.clusterPlugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

final class RlsPluginConfig implements PluginConfig {

    private static final String TYPE_URL = "type.googleapis.com/grpc.lookup.v1.RouteLookupClusterSpecifier";

    private final Map<String, ?> config;

    RlsPluginConfig(
            Map<String, ?> config) {
        if (config == null) {
            throw new NullPointerException("Null config");
        }
        this.config = Collections.unmodifiableMap(new HashMap<>(config));
    }

    Map<String, ?> config() {
        return config;
    }

    static RlsPluginConfig create(Map<String, ?> config) {
        return new RlsPluginConfig(config);
    }

    public String typeUrl() {
        return TYPE_URL;
    }

    @Override
    public String toString() {
        return "RlsPluginConfig{" + "config=" + config + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof RlsPluginConfig) {
            RlsPluginConfig that = (RlsPluginConfig) o;
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
