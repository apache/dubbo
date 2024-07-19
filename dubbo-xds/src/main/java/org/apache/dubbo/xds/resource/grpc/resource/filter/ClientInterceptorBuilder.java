package org.apache.dubbo.xds.resource.grpc.resource.filter;

import org.apache.dubbo.common.lang.Nullable;

import io.grpc.ClientInterceptor;
import io.grpc.LoadBalancer.PickSubchannelArgs;

import java.util.concurrent.ScheduledExecutorService;

public interface ClientInterceptorBuilder {
    @Nullable
    ClientInterceptor buildClientInterceptor(
        FilterConfig config, @Nullable FilterConfig overrideConfig, PickSubchannelArgs args,
        ScheduledExecutorService scheduler);
  }
