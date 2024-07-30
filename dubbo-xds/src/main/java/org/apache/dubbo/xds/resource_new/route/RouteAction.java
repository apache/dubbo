package org.apache.dubbo.xds.resource_new.route;

import org.apache.dubbo.common.lang.Nullable;
import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.xds.resource_new.route.plugin.NamedPluginConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RouteAction {

    private final List<HashPolicy> hashPolicies;

    @Nullable
    private final Long timeoutNano;

    @Nullable
    private final String cluster;

    @Nullable
    private final List<ClusterWeight> weightedClusters;

    @Nullable
    private final NamedPluginConfig namedClusterSpecifierPluginConfig;

    @Nullable
    private final RetryPolicy retryPolicy;

    public static RouteAction forCluster(
            String cluster,
            List<HashPolicy> hashPolicies,
            @Nullable Long timeoutNano,
            @Nullable RetryPolicy retryPolicy) {
        Assert.notNull(cluster, "cluster must not be null");
        return create(hashPolicies, timeoutNano, cluster, null, null, retryPolicy);
    }

    public static RouteAction forWeightedClusters(
            List<ClusterWeight> weightedClusters,
            List<HashPolicy> hashPolicies,
            @Nullable Long timeoutNano,
            @Nullable RetryPolicy retryPolicy) {
        Assert.notNull(weightedClusters, "weightedClusters must not be null");
        Assert.assertTrue(!weightedClusters.isEmpty(), "empty cluster list");
        return create(hashPolicies, timeoutNano, null, weightedClusters, null, retryPolicy);
    }

    public static RouteAction forClusterSpecifierPlugin(
            NamedPluginConfig namedConfig,
            List<HashPolicy> hashPolicies,
            @Nullable Long timeoutNano,
            @Nullable RetryPolicy retryPolicy) {
        Assert.notNull(namedConfig, "namedConfig must not be null");
        return create(hashPolicies, timeoutNano, null, null, namedConfig, retryPolicy);
    }

    private static RouteAction create(
            List<HashPolicy> hashPolicies,
            @Nullable Long timeoutNano,
            @Nullable String cluster,
            @Nullable List<ClusterWeight> weightedClusters,
            @Nullable NamedPluginConfig namedConfig,
            @Nullable RetryPolicy retryPolicy) {
        return new RouteAction(Collections.unmodifiableList(new ArrayList<>(hashPolicies)), timeoutNano, cluster,
                weightedClusters == null ? null : Collections.unmodifiableList(new ArrayList<>(weightedClusters)), namedConfig, retryPolicy);
    }

    RouteAction(
            List<HashPolicy> hashPolicies,
            @Nullable Long timeoutNano,
            @Nullable String cluster,
            @Nullable List<ClusterWeight> weightedClusters,
            @Nullable NamedPluginConfig namedClusterSpecifierPluginConfig,
            @Nullable RetryPolicy retryPolicy) {
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

    List<HashPolicy> hashPolicies() {
        return hashPolicies;
    }

    @Nullable
    Long timeoutNano() {
        return timeoutNano;
    }

    @Nullable
    String cluster() {
        return cluster;
    }

    @Nullable
    List<ClusterWeight> weightedClusters() {
        return weightedClusters;
    }

    @Nullable
    NamedPluginConfig namedClusterSpecifierPluginConfig() {
        return namedClusterSpecifierPluginConfig;
    }

    @Nullable
    RetryPolicy retryPolicy() {
        return retryPolicy;
    }

    public String toString() {
        return "RouteAction{" + "hashPolicies=" + hashPolicies + ", " + "timeoutNano=" + timeoutNano + ", " + "cluster="
                + cluster + ", " + "weightedClusters=" + weightedClusters + ", " + "namedClusterSpecifierPluginConfig="
                + namedClusterSpecifierPluginConfig + ", " + "retryPolicy=" + retryPolicy + "}";
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof RouteAction) {
            RouteAction that = (RouteAction) o;
            return this.hashPolicies.equals(that.hashPolicies()) && (
                    this.timeoutNano == null ? that.timeoutNano() == null : this.timeoutNano.equals(that.timeoutNano()))
                    && (this.cluster == null ? that.cluster() == null : this.cluster.equals(that.cluster())) && (
                    this.weightedClusters == null ?
                            that.weightedClusters() == null : this.weightedClusters.equals(that.weightedClusters()))
                    && (
                    this.namedClusterSpecifierPluginConfig == null ? that.namedClusterSpecifierPluginConfig()
                            == null :
                            this.namedClusterSpecifierPluginConfig.equals(that.namedClusterSpecifierPluginConfig()))
                    && (
                    this.retryPolicy == null ?
                            that.retryPolicy() == null : this.retryPolicy.equals(that.retryPolicy()));
        }
        return false;
    }

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
