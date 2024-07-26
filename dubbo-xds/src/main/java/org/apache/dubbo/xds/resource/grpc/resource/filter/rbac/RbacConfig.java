package org.apache.dubbo.xds.resource.grpc.resource.filter.rbac;

import org.apache.dubbo.common.lang.Nullable;
import org.apache.dubbo.xds.resource.grpc.resource.filter.FilterConfig;

final class RbacConfig implements FilterConfig {

    @Nullable
    private final AuthConfig authConfig;

    @Override
    public final String typeUrl() {
        return RbacFilter.TYPE_URL;
    }

    static RbacConfig create(@Nullable AuthConfig authConfig) {
        return new RbacConfig(authConfig);
    }

    RbacConfig(
            @Nullable AuthConfig authConfig) {
        this.authConfig = authConfig;
    }

    @Nullable
    AuthConfig authConfig() {
        return authConfig;
    }

    @Override
    public String toString() {
        return "RbacConfig{" + "authConfig=" + authConfig + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof RbacConfig) {
            RbacConfig that = (RbacConfig) o;
            return (this.authConfig == null ? that.authConfig() == null : this.authConfig.equals(that.authConfig()));
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= (authConfig == null) ? 0 : authConfig.hashCode();
        return h$;
    }

}
