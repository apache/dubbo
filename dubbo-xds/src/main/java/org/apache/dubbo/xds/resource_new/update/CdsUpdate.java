package org.apache.dubbo.xds.resource_new.update;

import org.apache.dubbo.common.lang.Nullable;
import org.apache.dubbo.xds.bootstrap.Bootstrapper;
import org.apache.dubbo.xds.bootstrap.Bootstrapper.ServerInfo;
import org.apache.dubbo.xds.resource_new.cluster.OutlierDetection;
import org.apache.dubbo.xds.resource_new.listener.security.UpstreamTlsContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CdsUpdate implements ResourceUpdate {

    enum ClusterType {
        EDS,
        LOGICAL_DNS,
        AGGREGATE
    }

    enum LbPolicy {
        ROUND_ROBIN,
        RING_HASH,
        LEAST_REQUEST
    }

    public static Builder forAggregate(String clusterName, List<String> prioritizedClusterNames) {
        if (prioritizedClusterNames == null) {
            throw new IllegalArgumentException("prioritizedClusterNames must not be null");
        }
        return new Builder().clusterName(clusterName)
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
        return new Builder().clusterName(clusterName)
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
        return new Builder().clusterName(clusterName)
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

    String clusterName() {
        return clusterName;
    }

    CdsUpdate.ClusterType clusterType() {
        return clusterType;
    }

    Map<String, ?> lbPolicyConfig() {
        return lbPolicyConfig;
    }

    long minRingSize() {
        return minRingSize;
    }

    long maxRingSize() {
        return maxRingSize;
    }

    int choiceCount() {
        return choiceCount;
    }

    @Nullable
    String edsServiceName() {
        return edsServiceName;
    }

    @Nullable
    String dnsHostName() {
        return dnsHostName;
    }

    @Nullable
    Bootstrapper.ServerInfo lrsServerInfo() {
        return lrsServerInfo;
    }

    @Nullable
    Long maxConcurrentRequests() {
        return maxConcurrentRequests;
    }

    @Nullable
    UpstreamTlsContext upstreamTlsContext() {
        return upstreamTlsContext;
    }

    @Nullable
    List<String> prioritizedClusterNames() {
        return prioritizedClusterNames;
    }

    @Nullable
    OutlierDetection outlierDetection() {
        return outlierDetection;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof CdsUpdate) {
            CdsUpdate that = (CdsUpdate) o;
            return this.clusterName.equals(that.clusterName()) && this.clusterType.equals(that.clusterType())
                    && this.lbPolicyConfig.equals(that.lbPolicyConfig()) && this.minRingSize == that.minRingSize()
                    && this.maxRingSize == that.maxRingSize() && this.choiceCount == that.choiceCount() && (
                    this.edsServiceName == null ?
                            that.edsServiceName() == null : this.edsServiceName.equals(that.edsServiceName())) && (
                    this.dnsHostName == null ? that.dnsHostName() == null : this.dnsHostName.equals(that.dnsHostName()))
                    && (
                    this.lrsServerInfo == null ?
                            that.lrsServerInfo() == null : this.lrsServerInfo.equals(that.lrsServerInfo())) && (
                    this.maxConcurrentRequests == null ? that.maxConcurrentRequests()
                            == null : this.maxConcurrentRequests.equals(that.maxConcurrentRequests())) && (
                    this.upstreamTlsContext == null ? that.upstreamTlsContext()
                            == null : this.upstreamTlsContext.equals(that.upstreamTlsContext())) && (
                    this.prioritizedClusterNames == null ? that.prioritizedClusterNames()
                            == null : this.prioritizedClusterNames.equals(that.prioritizedClusterNames())) && (
                    this.outlierDetection == null ?
                            that.outlierDetection() == null : this.outlierDetection.equals(that.outlierDetection()));
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

        public Builder() {
        }

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
            return new CdsUpdate(this.clusterName, this.clusterType, this.lbPolicyConfig, this.minRingSize,
                    this.maxRingSize, this.choiceCount, this.edsServiceName, this.dnsHostName, this.lrsServerInfo,
                    this.maxConcurrentRequests, this.upstreamTlsContext, this.prioritizedClusterNames,
                    this.outlierDetection);
        }
    }

}
