package org.apache.dubbo.xds.resource.grpc.resource.envoy.serverProtoData;

import java.util.Objects;

import io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.CommonTlsContext;

public class DownstreamTlsContext extends BaseTlsContext {

    private final boolean requireClientCertificate;

    public DownstreamTlsContext(
            CommonTlsContext commonTlsContext, boolean requireClientCertificate) {
        super(commonTlsContext);
        this.requireClientCertificate = requireClientCertificate;
    }

    public static DownstreamTlsContext fromEnvoyProtoDownstreamTlsContext(
            io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.DownstreamTlsContext downstreamTlsContext) {
        return new DownstreamTlsContext(downstreamTlsContext.getCommonTlsContext(),
                downstreamTlsContext.hasRequireClientCertificate());
    }

    public boolean isRequireClientCertificate() {
        return requireClientCertificate;
    }

    @Override
    public String toString() {
        return "DownstreamTlsContext{" + "commonTlsContext=" + commonTlsContext + ", requireClientCertificate="
                + requireClientCertificate + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        DownstreamTlsContext that = (DownstreamTlsContext) o;
        return requireClientCertificate == that.requireClientCertificate;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), requireClientCertificate);
    }
}
