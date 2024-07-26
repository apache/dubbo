package org.apache.dubbo.xds.resource.grpc.resource.envoy.serverProtoData;

import io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.CommonTlsContext;

import org.apache.dubbo.common.lang.Nullable;

import java.util.Objects;

public abstract class BaseTlsContext {
    @Nullable
    protected final CommonTlsContext commonTlsContext;

    protected BaseTlsContext(@Nullable CommonTlsContext commonTlsContext) {
        this.commonTlsContext = commonTlsContext;
    }

    @Nullable public CommonTlsContext getCommonTlsContext() {
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
