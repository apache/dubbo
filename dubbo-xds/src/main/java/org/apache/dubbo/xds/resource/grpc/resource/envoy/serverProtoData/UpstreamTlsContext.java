package org.apache.dubbo.xds.resource.grpc.resource.envoy.serverProtoData;

import io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.CommonTlsContext;

public final class UpstreamTlsContext extends BaseTlsContext {

    public UpstreamTlsContext(CommonTlsContext commonTlsContext) {
      super(commonTlsContext);
    }

    public static UpstreamTlsContext fromEnvoyProtoUpstreamTlsContext(
        io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.UpstreamTlsContext
            upstreamTlsContext) {
      return new UpstreamTlsContext(upstreamTlsContext.getCommonTlsContext());
    }

    @Override
    public String toString() {
      return "UpstreamTlsContext{" + "commonTlsContext=" + commonTlsContext + '}';
    }
}
