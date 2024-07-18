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

import com.google.common.base.MoreObjects;
import com.google.protobuf.Message;
import io.grpc.ClientInterceptor;
import io.grpc.LoadBalancer.PickSubchannelArgs;
import io.grpc.ServerInterceptor;

import javax.annotation.Nullable;

import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Defines the parsing functionality of an HTTP filter. A Filter may optionally implement either
 * {@link ClientInterceptorBuilder} or {@link ServerInterceptorBuilder} or both, indicating it is
 * capable of working on the client side or server side or both, respectively.
 */
interface Filter {

  /**
   * The proto message types supported by this filter. A filter will be registered by each of its
   * supported message types.
   */
  String[] typeUrls();

  /**
   * Parses the top-level filter config from raw proto message. The message may be either a {@link
   * com.google.protobuf.Any} or a {@link com.google.protobuf.Struct}.
   */
  ConfigOrError<? extends FilterConfig> parseFilterConfig(Message rawProtoMessage);

  /**
   * Parses the per-filter override filter config from raw proto message. The message may be either
   * a {@link com.google.protobuf.Any} or a {@link com.google.protobuf.Struct}.
   */
  ConfigOrError<? extends FilterConfig> parseFilterConfigOverride(Message rawProtoMessage);

  /** Represents an opaque data structure holding configuration for a filter. */
  interface FilterConfig {
    String typeUrl();
  }

  /** Uses the FilterConfigs produced above to produce an HTTP filter interceptor for clients. */
  interface ClientInterceptorBuilder {
    @Nullable
    ClientInterceptor buildClientInterceptor(
        FilterConfig config, @Nullable FilterConfig overrideConfig, PickSubchannelArgs args,
        ScheduledExecutorService scheduler);
  }

  /** Uses the FilterConfigs produced above to produce an HTTP filter interceptor for the server. */
  interface ServerInterceptorBuilder {
    @Nullable
    ServerInterceptor buildServerInterceptor(
        FilterConfig config, @Nullable FilterConfig overrideConfig);
  }

  /** Filter config with instance name. */
  final class NamedFilterConfig {
    // filter instance name
    final String name;
    final FilterConfig filterConfig;

    NamedFilterConfig(String name, FilterConfig filterConfig) {
      this.name = name;
      this.filterConfig = filterConfig;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      NamedFilterConfig that = (NamedFilterConfig) o;
      return Objects.equals(name, that.name)
          && Objects.equals(filterConfig, that.filterConfig);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, filterConfig);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("name", name)
          .add("filterConfig", filterConfig)
          .toString();
    }
  }
}
