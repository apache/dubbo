/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
        return new RouteAction(
                Collections.unmodifiableList(new ArrayList<>(hashPolicies)),
                timeoutNano,
                cluster,
                weightedClusters == null ? null : Collections.unmodifiableList(new ArrayList<>(weightedClusters)),
                namedConfig,
                retryPolicy);
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

    public List<HashPolicy> getHashPolicies() {
        return hashPolicies;
    }

    @Nullable
    public Long getTimeoutNano() {
        return timeoutNano;
    }

    @Nullable
    public String getCluster() {
        return cluster;
    }

    @Nullable
    public List<ClusterWeight> getWeightedClusters() {
        return weightedClusters;
    }

    @Nullable
    public NamedPluginConfig getNamedClusterSpecifierPluginConfig() {
        return namedClusterSpecifierPluginConfig;
    }

    @Nullable
    public RetryPolicy getRetryPolicy() {
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
            return this.hashPolicies.equals(that.getHashPolicies())
                    && (this.timeoutNano == null
                            ? that.getTimeoutNano() == null
                            : this.timeoutNano.equals(that.getTimeoutNano()))
                    && (this.cluster == null ? that.getCluster() == null : this.cluster.equals(that.getCluster()))
                    && (this.weightedClusters == null
                            ? that.getWeightedClusters() == null
                            : this.weightedClusters.equals(that.getWeightedClusters()))
                    && (this.namedClusterSpecifierPluginConfig == null
                            ? that.getNamedClusterSpecifierPluginConfig() == null
                            : this.namedClusterSpecifierPluginConfig.equals(
                                    that.getNamedClusterSpecifierPluginConfig()))
                    && (this.retryPolicy == null
                            ? that.getRetryPolicy() == null
                            : this.retryPolicy.equals(that.getRetryPolicy()));
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
