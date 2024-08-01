package org.apache.dubbo.xds.resource.grpc.resource.filter;

import org.apache.dubbo.common.lang.Nullable;

import io.grpc.ServerInterceptor;

public interface ServerInterceptorBuilder {
    @Nullable
    ServerInterceptor buildServerInterceptor(
        FilterConfig config, @Nullable FilterConfig overrideConfig);
  }
