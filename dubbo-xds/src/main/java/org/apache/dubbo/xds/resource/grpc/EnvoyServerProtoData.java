/*
 * Copyright 2020 The gRPC Authors
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

import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.util.Durations;
import org.apache.dubbo.xds.resource.grpc.HttpConnectionManager;
import io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.CommonTlsContext;
import io.grpc.Internal;

import javax.annotation.Nullable;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

/**
 * Defines gRPC data types for Envoy protobuf messages used in xDS protocol on the server side,
 * similar to how {@link EnvoyProtoData} defines it for the client side.
 */
@Internal
public final class EnvoyServerProtoData {

  // Prevent instantiation.
  private EnvoyServerProtoData() {
  }

  public abstract static class BaseTlsContext {
    @Nullable protected final CommonTlsContext commonTlsContext;

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

  public static final class UpstreamTlsContext extends BaseTlsContext {

    @VisibleForTesting
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

  public static final class DownstreamTlsContext extends BaseTlsContext {

    private final boolean requireClientCertificate;

    @VisibleForTesting
    public DownstreamTlsContext(
        CommonTlsContext commonTlsContext, boolean requireClientCertificate) {
      super(commonTlsContext);
      this.requireClientCertificate = requireClientCertificate;
    }

    public static DownstreamTlsContext fromEnvoyProtoDownstreamTlsContext(
        io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.DownstreamTlsContext
            downstreamTlsContext) {
      return new DownstreamTlsContext(downstreamTlsContext.getCommonTlsContext(),
        downstreamTlsContext.hasRequireClientCertificate());
    }

    public boolean isRequireClientCertificate() {
      return requireClientCertificate;
    }

    @Override
    public String toString() {
      return "DownstreamTlsContext{"
          + "commonTlsContext="
          + commonTlsContext
          + ", requireClientCertificate="
          + requireClientCertificate
          + '}';
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

  abstract static class CidrRange {

    abstract InetAddress addressPrefix();

    abstract int prefixLen();

      static CidrRange create(String addressPrefix, int prefixLen) throws UnknownHostException {
          return new AutoValue_EnvoyServerProtoData_CidrRange(
                  InetAddress.getByName(addressPrefix), prefixLen);
      }

  }

  enum ConnectionSourceType {
    // Any connection source matches.
    ANY,

    // Match a connection originating from the same host.
    SAME_IP_OR_LOOPBACK,

    // Match a connection originating from a different host.
    EXTERNAL
  }

  /**
   * Corresponds to Envoy proto message
   * {@link io.envoyproxy.envoy.config.listener.v3.FilterChainMatch}.
   */
  abstract static class FilterChainMatch {

    abstract int destinationPort();

    abstract ImmutableList<CidrRange> prefixRanges();

    abstract ImmutableList<String> applicationProtocols();

    abstract ImmutableList<CidrRange> sourcePrefixRanges();

    abstract ConnectionSourceType connectionSourceType();

    abstract ImmutableList<Integer> sourcePorts();

    abstract ImmutableList<String> serverNames();

    abstract String transportProtocol();

    public static FilterChainMatch create(int destinationPort,
        ImmutableList<CidrRange> prefixRanges,
        ImmutableList<String> applicationProtocols, ImmutableList<CidrRange> sourcePrefixRanges,
        ConnectionSourceType connectionSourceType, ImmutableList<Integer> sourcePorts,
        ImmutableList<String> serverNames, String transportProtocol) {
      return new AutoValue_EnvoyServerProtoData_FilterChainMatch(
          destinationPort, prefixRanges, applicationProtocols, sourcePrefixRanges,
          connectionSourceType, sourcePorts, serverNames, transportProtocol);
    }
  }

  /**
   * Corresponds to Envoy proto message {@link io.envoyproxy.envoy.config.listener.v3.FilterChain}.
   */
  abstract static class FilterChain {

    // possibly empty
    abstract String name();

    // TODO(sanjaypujare): flatten structure by moving FilterChainMatch class members here.
    abstract FilterChainMatch filterChainMatch();

    abstract HttpConnectionManager httpConnectionManager();

    @Nullable
    abstract SslContextProviderSupplier sslContextProviderSupplier();

    static FilterChain create(
        String name,
        FilterChainMatch filterChainMatch,
        HttpConnectionManager httpConnectionManager,
        @Nullable DownstreamTlsContext downstreamTlsContext,
        TlsContextManager tlsContextManager) {
      SslContextProviderSupplier sslContextProviderSupplier =
          downstreamTlsContext == null
              ? null : new SslContextProviderSupplier(downstreamTlsContext, tlsContextManager);
      return new AutoValue_EnvoyServerProtoData_FilterChain(
          name, filterChainMatch, httpConnectionManager, sslContextProviderSupplier);
    }
  }

  /**
   * Corresponds to Envoy proto message {@link io.envoyproxy.envoy.config.listener.v3.Listener} and
   * related classes.
   */
  abstract static class Listener {

    abstract String name();

    @Nullable
    abstract String address();

    abstract ImmutableList<FilterChain> filterChains();

    @Nullable
    abstract FilterChain defaultFilterChain();

    static Listener create(
        String name,
        @Nullable String address,
        ImmutableList<FilterChain> filterChains,
        @Nullable FilterChain defaultFilterChain) {
      return new AutoValue_EnvoyServerProtoData_Listener(name, address, filterChains,
          defaultFilterChain);
    }
  }

  /**
   * Corresponds to Envoy proto message {@link
   * io.envoyproxy.envoy.config.cluster.v3.OutlierDetection}. Only the fields supported by gRPC are
   * included.
   *
   * <p>Protobuf Duration fields are represented in their string format (e.g. "10s").
   */
  @AutoValue
  abstract static class OutlierDetection {

    @Nullable
    abstract Long intervalNanos();

    @Nullable
    abstract Long baseEjectionTimeNanos();

    @Nullable
    abstract Long maxEjectionTimeNanos();

    @Nullable
    abstract Integer maxEjectionPercent();

    @Nullable
    abstract SuccessRateEjection successRateEjection();

    @Nullable
    abstract FailurePercentageEjection failurePercentageEjection();

    static OutlierDetection create(
        @Nullable Long intervalNanos,
        @Nullable Long baseEjectionTimeNanos,
        @Nullable Long maxEjectionTimeNanos,
        @Nullable Integer maxEjectionPercentage,
        @Nullable SuccessRateEjection successRateEjection,
        @Nullable FailurePercentageEjection failurePercentageEjection) {
      return new AutoValue_EnvoyServerProtoData_OutlierDetection(intervalNanos,
          baseEjectionTimeNanos, maxEjectionTimeNanos, maxEjectionPercentage, successRateEjection,
          failurePercentageEjection);
    }

    static OutlierDetection fromEnvoyOutlierDetection(
        io.envoyproxy.envoy.config.cluster.v3.OutlierDetection envoyOutlierDetection) {

      Long intervalNanos = envoyOutlierDetection.hasInterval()
          ? Durations.toNanos(envoyOutlierDetection.getInterval()) : null;
      Long baseEjectionTimeNanos = envoyOutlierDetection.hasBaseEjectionTime()
          ? Durations.toNanos(envoyOutlierDetection.getBaseEjectionTime()) : null;
      Long maxEjectionTimeNanos = envoyOutlierDetection.hasMaxEjectionTime()
          ? Durations.toNanos(envoyOutlierDetection.getMaxEjectionTime()) : null;
      Integer maxEjectionPercentage = envoyOutlierDetection.hasMaxEjectionPercent()
          ? envoyOutlierDetection.getMaxEjectionPercent().getValue() : null;

      SuccessRateEjection successRateEjection;
      // If success rate enforcement has been turned completely off, don't configure this ejection.
      if (envoyOutlierDetection.hasEnforcingSuccessRate()
          && envoyOutlierDetection.getEnforcingSuccessRate().getValue() == 0) {
        successRateEjection = null;
      } else {
        Integer stdevFactor = envoyOutlierDetection.hasSuccessRateStdevFactor()
            ? envoyOutlierDetection.getSuccessRateStdevFactor().getValue() : null;
        Integer enforcementPercentage = envoyOutlierDetection.hasEnforcingSuccessRate()
            ? envoyOutlierDetection.getEnforcingSuccessRate().getValue() : null;
        Integer minimumHosts = envoyOutlierDetection.hasSuccessRateMinimumHosts()
            ? envoyOutlierDetection.getSuccessRateMinimumHosts().getValue() : null;
        Integer requestVolume = envoyOutlierDetection.hasSuccessRateRequestVolume()
            ? envoyOutlierDetection.getSuccessRateMinimumHosts().getValue() : null;

        successRateEjection = SuccessRateEjection.create(stdevFactor, enforcementPercentage,
            minimumHosts, requestVolume);
      }

      FailurePercentageEjection failurePercentageEjection;
      if (envoyOutlierDetection.hasEnforcingFailurePercentage()
          && envoyOutlierDetection.getEnforcingFailurePercentage().getValue() == 0) {
        failurePercentageEjection = null;
      } else {
        Integer threshold = envoyOutlierDetection.hasFailurePercentageThreshold()
            ? envoyOutlierDetection.getFailurePercentageThreshold().getValue() : null;
        Integer enforcementPercentage = envoyOutlierDetection.hasEnforcingFailurePercentage()
            ? envoyOutlierDetection.getEnforcingFailurePercentage().getValue() : null;
        Integer minimumHosts = envoyOutlierDetection.hasFailurePercentageMinimumHosts()
            ? envoyOutlierDetection.getFailurePercentageMinimumHosts().getValue() : null;
        Integer requestVolume = envoyOutlierDetection.hasFailurePercentageRequestVolume()
            ? envoyOutlierDetection.getFailurePercentageRequestVolume().getValue() : null;

        failurePercentageEjection = FailurePercentageEjection.create(threshold,
            enforcementPercentage, minimumHosts, requestVolume);
      }

      return create(intervalNanos, baseEjectionTimeNanos, maxEjectionTimeNanos,
          maxEjectionPercentage, successRateEjection, failurePercentageEjection);
    }
  }

  @AutoValue
  abstract static class SuccessRateEjection {

    @Nullable
    abstract Integer stdevFactor();

    @Nullable
    abstract Integer enforcementPercentage();

    @Nullable
    abstract Integer minimumHosts();

    @Nullable
    abstract Integer requestVolume();

    static SuccessRateEjection create(
        @Nullable Integer stdevFactor,
        @Nullable Integer enforcementPercentage,
        @Nullable Integer minimumHosts,
        @Nullable Integer requestVolume) {
      return new AutoValue_EnvoyServerProtoData_SuccessRateEjection(stdevFactor,
          enforcementPercentage, minimumHosts, requestVolume);
    }
  }

  @AutoValue
  abstract static class FailurePercentageEjection {

    @Nullable
    abstract Integer threshold();

    @Nullable
    abstract Integer enforcementPercentage();

    @Nullable
    abstract Integer minimumHosts();

    @Nullable
    abstract Integer requestVolume();

    static FailurePercentageEjection create(
        @Nullable Integer threshold,
        @Nullable Integer enforcementPercentage,
        @Nullable Integer minimumHosts,
        @Nullable Integer requestVolume) {
      return new AutoValue_EnvoyServerProtoData_FailurePercentageEjection(threshold,
          enforcementPercentage, minimumHosts, requestVolume);
    }
  }
}
