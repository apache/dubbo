package org.apache.dubbo.xds.resource.grpc;

import org.apache.dubbo.common.lang.Nullable;

import com.google.common.collect.ImmutableList;

final class AutoValue_VirtualHost_Route_RouteAction extends VirtualHost.Route.RouteAction {

  private final ImmutableList<HashPolicy> hashPolicies;

  @Nullable
  private final Long timeoutNano;

  @Nullable
  private final String cluster;

  @Nullable
  private final ImmutableList<VirtualHost.Route.RouteAction.ClusterWeight> weightedClusters;

  @Nullable
  private final ClusterSpecifierPlugin.NamedPluginConfig namedClusterSpecifierPluginConfig;

  @Nullable
  private final VirtualHost.Route.RouteAction.RetryPolicy retryPolicy;

  AutoValue_VirtualHost_Route_RouteAction(
      ImmutableList<VirtualHost.Route.RouteAction.HashPolicy> hashPolicies,
      @Nullable Long timeoutNano,
      @Nullable String cluster,
      @Nullable ImmutableList<VirtualHost.Route.RouteAction.ClusterWeight> weightedClusters,
      @Nullable ClusterSpecifierPlugin.NamedPluginConfig namedClusterSpecifierPluginConfig,
      @Nullable VirtualHost.Route.RouteAction.RetryPolicy retryPolicy) {
    if (hashPolicies == null) {
      throw new NullPointerException("Null hashPolicies");
    }
    this.hashPolicies = hashPolicies;
    this.timeoutNano = timeoutNano;
    this.cluster = cluster;
    this.weightedClusters = weightedClusters;
    this.namedClusterSpecifierPluginConfig = namedClusterSpecifierPluginConfig;
    this.retryPolicy = retryPolicy;
  }

  @Override
  ImmutableList<VirtualHost.Route.RouteAction.HashPolicy> hashPolicies() {
    return hashPolicies;
  }

  @Nullable
  @Override
  Long timeoutNano() {
    return timeoutNano;
  }

  @Nullable
  @Override
  String cluster() {
    return cluster;
  }

  @Nullable
  @Override
  ImmutableList<VirtualHost.Route.RouteAction.ClusterWeight> weightedClusters() {
    return weightedClusters;
  }

  @Nullable
  @Override
  ClusterSpecifierPlugin.NamedPluginConfig namedClusterSpecifierPluginConfig() {
    return namedClusterSpecifierPluginConfig;
  }

  @Nullable
  @Override
  VirtualHost.Route.RouteAction.RetryPolicy retryPolicy() {
    return retryPolicy;
  }

  @Override
  public String toString() {
    return "RouteAction{"
        + "hashPolicies=" + hashPolicies + ", "
        + "timeoutNano=" + timeoutNano + ", "
        + "cluster=" + cluster + ", "
        + "weightedClusters=" + weightedClusters + ", "
        + "namedClusterSpecifierPluginConfig=" + namedClusterSpecifierPluginConfig + ", "
        + "retryPolicy=" + retryPolicy
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof VirtualHost.Route.RouteAction) {
      VirtualHost.Route.RouteAction that = (VirtualHost.Route.RouteAction) o;
      return this.hashPolicies.equals(that.hashPolicies())
          && (this.timeoutNano == null ? that.timeoutNano() == null : this.timeoutNano.equals(that.timeoutNano()))
          && (this.cluster == null ? that.cluster() == null : this.cluster.equals(that.cluster()))
          && (this.weightedClusters == null ? that.weightedClusters() == null : this.weightedClusters.equals(that.weightedClusters()))
          && (this.namedClusterSpecifierPluginConfig == null ? that.namedClusterSpecifierPluginConfig() == null : this.namedClusterSpecifierPluginConfig.equals(that.namedClusterSpecifierPluginConfig()))
          && (this.retryPolicy == null ? that.retryPolicy() == null : this.retryPolicy.equals(that.retryPolicy()));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= hashPolicies.hashCode();
    h$ *= 1000003;
    h$ ^= (timeoutNano == null) ? 0 : timeoutNano.hashCode();
    h$ *= 1000003;
    h$ ^= (cluster == null) ? 0 : cluster.hashCode();
    h$ *= 1000003;
    h$ ^= (weightedClusters == null) ? 0 : weightedClusters.hashCode();
    h$ *= 1000003;
    h$ ^= (namedClusterSpecifierPluginConfig == null) ? 0 : namedClusterSpecifierPluginConfig.hashCode();
    h$ *= 1000003;
    h$ ^= (retryPolicy == null) ? 0 : retryPolicy.hashCode();
    return h$;
  }

}
