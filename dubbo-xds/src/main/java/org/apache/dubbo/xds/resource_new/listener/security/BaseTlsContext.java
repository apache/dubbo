package org.apache.dubbo.xds.resource_new.listener.security;

import org.apache.dubbo.common.lang.Nullable;

import java.util.Objects;

import io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.CommonTlsContext;

public abstract class BaseTlsContext {
    @Nullable
    protected final CommonTlsContext commonTlsContext;

    protected BaseTlsContext(@Nullable CommonTlsContext commonTlsContext) {
        this.commonTlsContext = commonTlsContext;
    }

    @Nullable
    public CommonTlsContext getCommonTlsContext() {
        return commonTlsContext;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BaseTlsContext)) {
            return false;
        }
        BaseTlsContext that = (BaseTlsContext) o;
        return Objects.equals(commonTlsContext, that.commonTlsContext);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(commonTlsContext);
    }
}
