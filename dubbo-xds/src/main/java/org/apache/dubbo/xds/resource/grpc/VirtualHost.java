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

import org.apache.dubbo.xds.resource.grpc.ClusterSpecifierPlugin.NamedPluginConfig;
import org.apache.dubbo.xds.resource.grpc.Filter.FilterConfig;
import org.apache.dubbo.xds.resource.grpc.Matchers.FractionMatcher;
import org.apache.dubbo.xds.resource.grpc.Matchers.HeaderMatcher;

import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Duration;
import com.google.re2j.Pattern;
import io.grpc.Status.Code;

import javax.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/** Represents an upstream virtual host. */
@AutoValue
abstract class VirtualHost {
  // The canonical name of this virtual host.
  abstract String name();

  // The list of domains (host/authority header) that will be matched to this virtual host.
  abstract ImmutableList<String> domains();

  // The list of routes that will be matched, in order, for incoming requests.
  abstract ImmutableList<Route> routes();

  abstract ImmutableMap<String, FilterConfig> filterConfigOverrides();

  public static VirtualHost create(
      String name, List<String> domains, List<Route> routes,
      Map<String, FilterConfig> filterConfigOverrides) {
    return new AutoValue_VirtualHost(name, ImmutableList.copyOf(domains),
        ImmutableList.copyOf(routes), ImmutableMap.copyOf(filterConfigOverrides));
  }

  @AutoValue
  abstract static class Route {
    abstract RouteMatch routeMatch();

    @Nullable
    abstract RouteAction routeAction();

    abstract ImmutableMap<String, FilterConfig> filterConfigOverrides();

    static Route forAction(RouteMatch routeMatch, RouteAction routeAction,
        Map<String, FilterConfig> filterConfigOverrides) {
      return create(routeMatch, routeAction, filterConfigOverrides);
    }

    static Route forNonForwardingAction(RouteMatch routeMatch,
        Map<String, FilterConfig> filterConfigOverrides) {
      return create(routeMatch, null, filterConfigOverrides);
    }

    private static Route create(
        RouteMatch routeMatch, @Nullable RouteAction routeAction,
        Map<String, FilterConfig> filterConfigOverrides) {
      return new AutoValue_VirtualHost_Route(
          routeMatch, routeAction, ImmutableMap.copyOf(filterConfigOverrides));
    }

    @AutoValue
    abstract static class RouteMatch {
      abstract PathMatcher pathMatcher();

      abstract ImmutableList<HeaderMatcher> headerMatchers();

      @Nullable
      abstract FractionMatcher fractionMatcher();

      // TODO(chengyuanzhang): maybe delete me.
      @VisibleForTesting
      static RouteMatch withPathExactOnly(String path) {
        return RouteMatch.create(PathMatcher.fromPath(path, true),
            Collections.<HeaderMatcher>emptyList(), null);
      }

      static RouteMatch create(PathMatcher pathMatcher,
          List<HeaderMatcher> headerMatchers, @Nullable FractionMatcher fractionMatcher) {
        return new AutoValue_VirtualHost_Route_RouteMatch(pathMatcher,
            ImmutableList.copyOf(headerMatchers), fractionMatcher);
      }

      /** Matcher for HTTP request path. */
      @AutoValue
      abstract static class PathMatcher {
        // Exact full path to be matched.
        @Nullable
        abstract String path();

        // Path prefix to be matched.
        @Nullable
        abstract String prefix();

        // Regular expression pattern of the path to be matched.
        @Nullable
        abstract Pattern regEx();

        // Whether case sensitivity is taken into account for matching.
        // Only valid for full path matching or prefix matching.
        abstract boolean caseSensitive();

        static PathMatcher fromPath(String path, boolean caseSensitive) {
          checkNotNull(path, "path");
          return create(path, null, null, caseSensitive);
        }

        static PathMatcher fromPrefix(String prefix, boolean caseSensitive) {
          checkNotNull(prefix, "prefix");
          return create(null, prefix, null, caseSensitive);
        }

        static PathMatcher fromRegEx(Pattern regEx) {
          checkNotNull(regEx, "regEx");
          return create(null, null, regEx, false /* doesn't matter */);
        }

        private static PathMatcher create(@Nullable String path, @Nullable String prefix,
            @Nullable Pattern regEx, boolean caseSensitive) {
          return new AutoValue_VirtualHost_Route_RouteMatch_PathMatcher(path, prefix, regEx,
              caseSensitive);
        }
      }
    }

    @AutoValue
    abstract static class RouteAction {
      // List of hash policies to use for ring hash load balancing.
      abstract ImmutableList<HashPolicy> hashPolicies();

      @Nullable
      abstract Long timeoutNano();

      @Nullable
      abstract String cluster();

      @Nullable
      abstract ImmutableList<ClusterWeight> weightedClusters();

      @Nullable
      abstract NamedPluginConfig namedClusterSpecifierPluginConfig();

      @Nullable
      abstract RetryPolicy retryPolicy();

      static RouteAction forCluster(
          String cluster, List<HashPolicy> hashPolicies, @Nullable Long timeoutNano,
          @Nullable RetryPolicy retryPolicy) {
        checkNotNull(cluster, "cluster");
        return RouteAction.create(hashPolicies, timeoutNano, cluster, null, null, retryPolicy);
      }

      static RouteAction forWeightedClusters(
          List<ClusterWeight> weightedClusters, List<HashPolicy> hashPolicies,
          @Nullable Long timeoutNano, @Nullable RetryPolicy retryPolicy) {
        checkNotNull(weightedClusters, "weightedClusters");
        checkArgument(!weightedClusters.isEmpty(), "empty cluster list");
        return RouteAction.create(
            hashPolicies, timeoutNano, null, weightedClusters, null, retryPolicy);
      }

      static RouteAction forClusterSpecifierPlugin(
          NamedPluginConfig namedConfig,
          List<HashPolicy> hashPolicies,
          @Nullable Long timeoutNano,
          @Nullable RetryPolicy retryPolicy) {
        checkNotNull(namedConfig, "namedConfig");
        return RouteAction.create(hashPolicies, timeoutNano, null, null, namedConfig, retryPolicy);
      }

      private static RouteAction create(
          List<HashPolicy> hashPolicies,
          @Nullable Long timeoutNano,
          @Nullable String cluster,
          @Nullable List<ClusterWeight> weightedClusters,
          @Nullable NamedPluginConfig namedConfig,
          @Nullable RetryPolicy retryPolicy) {
        return new AutoValue_VirtualHost_Route_RouteAction(
            ImmutableList.copyOf(hashPolicies),
            timeoutNano,
            cluster,
            weightedClusters == null ? null : ImmutableList.copyOf(weightedClusters),
            namedConfig,
            retryPolicy);
      }

      @AutoValue
      abstract static class ClusterWeight {
        abstract String name();

        abstract int weight();

        abstract ImmutableMap<String, FilterConfig> filterConfigOverrides();

        static ClusterWeight create(
            String name, int weight, Map<String, FilterConfig> filterConfigOverrides) {
          return new AutoValue_VirtualHost_Route_RouteAction_ClusterWeight(
              name, weight, ImmutableMap.copyOf(filterConfigOverrides));
        }
      }

      // Configuration for the route's hashing policy if the upstream cluster uses a hashing load
      // balancer.
      @AutoValue
      abstract static class HashPolicy {
        // The specifier that indicates the component of the request to be hashed on.
        abstract Type type();

        // The flag that short-circuits the hash computing.
        abstract boolean isTerminal();

        // The name of the request header that will be used to obtain the hash key.
        // Only valid if type is HEADER.
        @Nullable
        abstract String headerName();

        // The regular expression used to find portions to be replaced in the header value.
        // Only valid if type is HEADER.
        @Nullable
        abstract Pattern regEx();

        // The string that should be substituted into matching portions of the header value.
        // Only valid if type is HEADER.
        @Nullable
        abstract String regExSubstitution();

        static HashPolicy forHeader(boolean isTerminal, String headerName,
            @Nullable Pattern regEx, @Nullable String regExSubstitution) {
          checkNotNull(headerName, "headerName");
          return HashPolicy.create(Type.HEADER, isTerminal, headerName, regEx, regExSubstitution);
        }

        static HashPolicy forChannelId(boolean isTerminal) {
          return HashPolicy.create(Type.CHANNEL_ID, isTerminal, null, null, null);
        }

        private static HashPolicy create(Type type, boolean isTerminal, @Nullable String headerName,
            @Nullable Pattern regEx, @Nullable String regExSubstitution) {
          return new AutoValue_VirtualHost_Route_RouteAction_HashPolicy(type, isTerminal,
              headerName, regEx, regExSubstitution);
        }

        enum Type {
          HEADER, CHANNEL_ID
        }
      }

      @AutoValue
      abstract static class RetryPolicy {
        abstract int maxAttempts();

        abstract ImmutableList<Code> retryableStatusCodes();

        abstract Duration initialBackoff();

        abstract Duration maxBackoff();

        @Nullable
        abstract Duration perAttemptRecvTimeout();

        static RetryPolicy create(
            int maxAttempts, List<Code> retryableStatusCodes, Duration initialBackoff,
            Duration maxBackoff, @Nullable Duration perAttemptRecvTimeout) {
          return new AutoValue_VirtualHost_Route_RouteAction_RetryPolicy(
              maxAttempts,
              ImmutableList.copyOf(retryableStatusCodes),
              initialBackoff,
              maxBackoff,
              perAttemptRecvTimeout);
        }
      }
    }
  }
}
