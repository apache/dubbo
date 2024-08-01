/*
 * Copyright 2021 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.xds.resource.grpc;

import org.apache.dubbo.xds.resource.grpc.Filter.NamedFilterConfig;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * HttpConnectionManager is a network filter for proxying HTTP requests.
 */
@AutoValue
public abstract class HttpConnectionManager {
  // Total number of nanoseconds to keep alive an HTTP request/response stream.
  abstract long httpMaxStreamDurationNano();

  // Name of the route configuration to be used for RDS resource discovery.
  @Nullable
  abstract String rdsName();

  // List of virtual hosts that make up the route table.
  @Nullable
  abstract ImmutableList<VirtualHost> virtualHosts();

  // List of http filter configs. Null if HttpFilter support is not enabled.
  @Nullable
  abstract ImmutableList<NamedFilterConfig> httpFilterConfigs();

  static HttpConnectionManager forRdsName(long httpMaxStreamDurationNano, String rdsName,
      @Nullable List<NamedFilterConfig> httpFilterConfigs) {
    checkNotNull(rdsName, "rdsName");
    return create(httpMaxStreamDurationNano, rdsName, null, httpFilterConfigs);
  }

  static HttpConnectionManager forVirtualHosts(long httpMaxStreamDurationNano,
      List<VirtualHost> virtualHosts, @Nullable List<NamedFilterConfig> httpFilterConfigs) {
    checkNotNull(virtualHosts, "virtualHosts");
    return create(httpMaxStreamDurationNano, null, virtualHosts,
        httpFilterConfigs);
  }

  private static HttpConnectionManager create(long httpMaxStreamDurationNano,
      @Nullable String rdsName, @Nullable List<VirtualHost> virtualHosts,
      @Nullable List<NamedFilterConfig> httpFilterConfigs) {
    return new AutoValue_HttpConnectionManager(
        httpMaxStreamDurationNano, rdsName,
        virtualHosts == null ? null : ImmutableList.copyOf(virtualHosts),
        httpFilterConfigs == null ? null : ImmutableList.copyOf(httpFilterConfigs));
  }
}
