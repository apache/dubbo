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
package org.apache.dubbo.xds.resource.update;

import org.apache.dubbo.common.lang.Nullable;
import org.apache.dubbo.xds.bootstrap.Bootstrapper;
import org.apache.dubbo.xds.bootstrap.Bootstrapper.ServerInfo;
import org.apache.dubbo.xds.resource.cluster.OutlierDetection;
import org.apache.dubbo.xds.resource.listener.security.UpstreamTlsContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.envoyproxy.envoy.config.cluster.v3.Cluster;

public class CdsUpdate implements ResourceUpdate {

    public enum ClusterType {
        EDS,
        LOGICAL_DNS,
        AGGREGATE
    }

    public enum LbPolicy {
        ROUND_ROBIN,
        RING_HASH,
        LEAST_REQUEST
    }

    public static Builder forAggregate(String clusterName, List<String> prioritizedClusterNames) {
        if (prioritizedClusterNames == null) {
            throw new IllegalArgumentException("prioritizedClusterNames must not be null");
        }
        return new Builder()
                .clusterName(clusterName)
                .clusterType(ClusterType.AGGREGATE)
                .minRingSize(0)
                .maxRingSize(0)
                .choiceCount(0)
                .prioritizedClusterNames(prioritizedClusterNames);
    }

    public static Builder forEds(
            String clusterName,
            @Nullable String edsServiceName,
            @Nullable ServerInfo lrsServerInfo,
            @Nullable Long maxConcurrentRequests,
            @Nullable UpstreamTlsContext upstreamTlsContext,
            @Nullable OutlierDetection outlierDetection) {
        return new Builder()
                .clusterName(clusterName)
                .clusterType(ClusterType.EDS)
                .minRingSize(0)
                .maxRingSize(0)
                .choiceCount(0)
                .edsServiceName(edsServiceName)
                .lrsServerInfo(lrsServerInfo)
                .maxConcurrentRequests(maxConcurrentRequests)
                .upstreamTlsContext(upstreamTlsContext)
                .outlierDetection(outlierDetection);
    }

    public static Builder forLogicalDns(
            String clusterName,
            String dnsHostName,
            @Nullable ServerInfo lrsServerInfo,
            @Nullable Long maxConcurrentRequests,
            @Nullable UpstreamTlsContext upstreamTlsContext) {
        return new Builder()
                .clusterName(clusterName)
                .clusterType(ClusterType.LOGICAL_DNS)
                .minRingSize(0)
                .maxRingSize(0)
                .choiceCount(0)
                .dnsHostName(dnsHostName)
                .lrsServerInfo(lrsServerInfo)
                .maxConcurrentRequests(maxConcurrentRequests)
                .upstreamTlsContext(upstreamTlsContext);
    }

    private final String clusterName;

    private final ClusterType clusterType;

    private final Map<String, ?> lbPolicyConfig;

    private final long minRingSize;

    private final long maxRingSize;

    private final int choiceCount;

    @Nullable
    private final String edsServiceName;

    @Nullable
    private final String dnsHostName;

    @Nullable
    private final Bootstrapper.ServerInfo lrsServerInfo;

    @Nullable
    private final Long maxConcurrentRequests;

    @Nullable
    private final UpstreamTlsContext upstreamTlsContext;

    @Nullable
    private final List<String> prioritizedClusterNames;

    @Nullable
    private final OutlierDetection outlierDetection;

    private Cluster rawCluster;

    private CdsUpdate(
            String clusterName,
            ClusterType clusterType,
            Map<String, ?> lbPolicyConfig,
            long minRingSize,
            long maxRingSize,
            int choiceCount,
            @Nullable String edsServiceName,
            @Nullable String dnsHostName,
            @Nullable Bootstrapper.ServerInfo lrsServerInfo,
            @Nullable Long maxConcurrentRequests,
            @Nullable UpstreamTlsContext upstreamTlsContext,
            @Nullable List<String> prioritizedClusterNames,
            @Nullable OutlierDetection outlierDetection) {
        this.clusterName = clusterName;
        this.clusterType = clusterType;
        this.lbPolicyConfig = lbPolicyConfig;
        this.minRingSize = minRingSize;
        this.maxRingSize = maxRingSize;
        this.choiceCount = choiceCount;
        this.edsServiceName = edsServiceName;
        this.dnsHostName = dnsHostName;
        this.lrsServerInfo = lrsServerInfo;
        this.maxConcurrentRequests = maxConcurrentRequests;
        this.upstreamTlsContext = upstreamTlsContext;
        this.prioritizedClusterNames = Collections.unmodifiableList(new ArrayList<>(prioritizedClusterNames));
        this.outlierDetection = outlierDetection;
    }

    public String getClusterName() {
        return clusterName;
    }

    public CdsUpdate.ClusterType getClusterType() {
        return clusterType;
    }

    public Map<String, ?> getLbPolicyConfig() {
        return lbPolicyConfig;
    }

    public long getMinRingSize() {
        return minRingSize;
    }

    public long getMaxRingSize() {
        return maxRingSize;
    }

    public int getChoiceCount() {
        return choiceCount;
    }

    @Nullable
    public String getEdsServiceName() {
        return edsServiceName;
    }

    @Nullable
    public String getDnsHostName() {
        return dnsHostName;
    }

    @Nullable
    public Bootstrapper.ServerInfo getLrsServerInfo() {
        return lrsServerInfo;
    }

    @Nullable
    public Long getMaxConcurrentRequests() {
        return maxConcurrentRequests;
    }

    @Nullable
    public UpstreamTlsContext getUpstreamTlsContext() {
        return upstreamTlsContext;
    }

    @Nullable
    public List<String> getPrioritizedClusterNames() {
        return prioritizedClusterNames;
    }

    @Nullable
    public OutlierDetection getOutlierDetection() {
        return outlierDetection;
    }

    public Cluster getRawCluster() {
        return rawCluster;
    }

    public void setRawCluster(Cluster rawCluster) {
        this.rawCluster = rawCluster;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof CdsUpdate) {
            CdsUpdate that = (CdsUpdate) o;
            return this.clusterName.equals(that.getClusterName())
                    && this.clusterType.equals(that.getClusterType())
                    && this.lbPolicyConfig.equals(that.getLbPolicyConfig())
                    && this.minRingSize == that.getMinRingSize()
                    && this.maxRingSize == that.getMaxRingSize()
                    && this.choiceCount == that.getChoiceCount()
                    && (this.edsServiceName == null
                            ? that.getEdsServiceName() == null
                            : this.edsServiceName.equals(that.getEdsServiceName()))
                    && (this.dnsHostName == null
                            ? that.getDnsHostName() == null
                            : this.dnsHostName.equals(that.getDnsHostName()))
                    && (this.lrsServerInfo == null
                            ? that.getLrsServerInfo() == null
                            : this.lrsServerInfo.equals(that.getLrsServerInfo()))
                    && (this.maxConcurrentRequests == null
                            ? that.getMaxConcurrentRequests() == null
                            : this.maxConcurrentRequests.equals(that.getMaxConcurrentRequests()))
                    && (this.upstreamTlsContext == null
                            ? that.getUpstreamTlsContext() == null
                            : this.upstreamTlsContext.equals(that.getUpstreamTlsContext()))
                    && (this.prioritizedClusterNames == null
                            ? that.getPrioritizedClusterNames() == null
                            : this.prioritizedClusterNames.equals(that.getPrioritizedClusterNames()))
                    && (this.outlierDetection == null
                            ? that.getOutlierDetection() == null
                            : this.outlierDetection.equals(that.getOutlierDetection()));
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= clusterName.hashCode();
        h$ *= 1000003;
        h$ ^= clusterType.hashCode();
        h$ *= 1000003;
        h$ ^= lbPolicyConfig.hashCode();
        h$ *= 1000003;
        h$ ^= (int) ((minRingSize >>> 32) ^ minRingSize);
        h$ *= 1000003;
        h$ ^= (int) ((maxRingSize >>> 32) ^ maxRingSize);
        h$ *= 1000003;
        h$ ^= choiceCount;
        h$ *= 1000003;
        h$ ^= (edsServiceName == null) ? 0 : edsServiceName.hashCode();
        h$ *= 1000003;
        h$ ^= (dnsHostName == null) ? 0 : dnsHostName.hashCode();
        h$ *= 1000003;
        h$ ^= (lrsServerInfo == null) ? 0 : lrsServerInfo.hashCode();
        h$ *= 1000003;
        h$ ^= (maxConcurrentRequests == null) ? 0 : maxConcurrentRequests.hashCode();
        h$ *= 1000003;
        h$ ^= (upstreamTlsContext == null) ? 0 : upstreamTlsContext.hashCode();
        h$ *= 1000003;
        h$ ^= (prioritizedClusterNames == null) ? 0 : prioritizedClusterNames.hashCode();
        h$ *= 1000003;
        h$ ^= (outlierDetection == null) ? 0 : outlierDetection.hashCode();
        return h$;
    }

    public static class Builder {
        private String clusterName;
        private CdsUpdate.ClusterType clusterType;
        private Map<String, ?> lbPolicyConfig;
        private long minRingSize;
        private long maxRingSize;
        private int choiceCount;
        private String edsServiceName;
        private String dnsHostName;
        private Bootstrapper.ServerInfo lrsServerInfo;
        private Long maxConcurrentRequests;
        private UpstreamTlsContext upstreamTlsContext;
        private List<String> prioritizedClusterNames;
        private OutlierDetection outlierDetection;
        private byte set$0;

        public Builder() {}

        public Builder clusterName(String clusterName) {
            if (clusterName == null) {
                throw new NullPointerException("Null clusterName");
            }
            this.clusterName = clusterName;
            return this;
        }

        public Builder clusterType(ClusterType clusterType) {
            if (clusterType == null) {
                throw new NullPointerException("Null clusterType");
            }
            this.clusterType = clusterType;
            return this;
        }

        public Builder lbPolicyConfig(Map<String, ?> lbPolicyConfig) {
            if (lbPolicyConfig == null) {
                throw new NullPointerException("Null lbPolicyConfig");
            }
            this.lbPolicyConfig = lbPolicyConfig;
            return this;
        }

        public Builder minRingSize(long minRingSize) {
            this.minRingSize = minRingSize;
            set$0 |= (byte) 1;
            return this;
        }

        public Builder maxRingSize(long maxRingSize) {
            this.maxRingSize = maxRingSize;
            set$0 |= (byte) 2;
            return this;
        }

        public Builder choiceCount(int choiceCount) {
            this.choiceCount = choiceCount;
            set$0 |= (byte) 4;
            return this;
        }

        public Builder edsServiceName(String edsServiceName) {
            this.edsServiceName = edsServiceName;
            return this;
        }

        public Builder dnsHostName(String dnsHostName) {
            this.dnsHostName = dnsHostName;
            return this;
        }

        public Builder lrsServerInfo(Bootstrapper.ServerInfo lrsServerInfo) {
            this.lrsServerInfo = lrsServerInfo;
            return this;
        }

        public Builder maxConcurrentRequests(Long maxConcurrentRequests) {
            this.maxConcurrentRequests = maxConcurrentRequests;
            return this;
        }

        public Builder upstreamTlsContext(UpstreamTlsContext upstreamTlsContext) {
            this.upstreamTlsContext = upstreamTlsContext;
            return this;
        }

        public Builder prioritizedClusterNames(List<String> prioritizedClusterNames) {
            this.prioritizedClusterNames = prioritizedClusterNames;
            return this;
        }

        public Builder outlierDetection(OutlierDetection outlierDetection) {
            this.outlierDetection = outlierDetection;
            return this;
        }

        public CdsUpdate build() {
            if (set$0 != 7 || this.clusterName == null || this.clusterType == null || this.lbPolicyConfig == null) {
                StringBuilder missing = new StringBuilder();
                if (this.clusterName == null) {
                    missing.append(" clusterName");
                }
                if (this.clusterType == null) {
                    missing.append(" clusterType");
                }
                if (this.lbPolicyConfig == null) {
                    missing.append(" lbPolicyConfig");
                }
                if ((set$0 & 1) == 0) {
                    missing.append(" minRingSize");
                }
                if ((set$0 & 2) == 0) {
                    missing.append(" maxRingSize");
                }
                if ((set$0 & 4) == 0) {
                    missing.append(" choiceCount");
                }
                throw new IllegalStateException("Missing required properties:" + missing);
            }
            return new CdsUpdate(
                    this.clusterName,
                    this.clusterType,
                    this.lbPolicyConfig,
                    this.minRingSize,
                    this.maxRingSize,
                    this.choiceCount,
                    this.edsServiceName,
                    this.dnsHostName,
                    this.lrsServerInfo,
                    this.maxConcurrentRequests,
                    this.upstreamTlsContext,
                    this.prioritizedClusterNames,
                    this.outlierDetection);
        }
    }
}
