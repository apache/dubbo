package org.apache.dubbo.xds.resource.grpc;

import org.apache.dubbo.common.lang.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.List;

final class AutoValue_XdsClusterResource_CdsUpdate extends XdsClusterResource.CdsUpdate {

  private final String clusterName;

  private final XdsClusterResource.CdsUpdate.ClusterType clusterType;

  private final ImmutableMap<String, ?> lbPolicyConfig;

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
  private final EnvoyServerProtoData.UpstreamTlsContext upstreamTlsContext;

  @Nullable
  private final ImmutableList<String> prioritizedClusterNames;

  @Nullable
  private final EnvoyServerProtoData.OutlierDetection outlierDetection;

  private AutoValue_XdsClusterResource_CdsUpdate(
      String clusterName,
      XdsClusterResource.CdsUpdate.ClusterType clusterType,
      ImmutableMap<String, ?> lbPolicyConfig,
      long minRingSize,
      long maxRingSize,
      int choiceCount,
      @Nullable String edsServiceName,
      @Nullable String dnsHostName,
      @Nullable Bootstrapper.ServerInfo lrsServerInfo,
      @Nullable Long maxConcurrentRequests,
      @Nullable EnvoyServerProtoData.UpstreamTlsContext upstreamTlsContext,
      @Nullable ImmutableList<String> prioritizedClusterNames,
      @Nullable EnvoyServerProtoData.OutlierDetection outlierDetection) {
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
    this.prioritizedClusterNames = prioritizedClusterNames;
    this.outlierDetection = outlierDetection;
  }

  @Override
  String clusterName() {
    return clusterName;
  }

  @Override
  XdsClusterResource.CdsUpdate.ClusterType clusterType() {
    return clusterType;
  }

  @Override
  ImmutableMap<String, ?> lbPolicyConfig() {
    return lbPolicyConfig;
  }

  @Override
  long minRingSize() {
    return minRingSize;
  }

  @Override
  long maxRingSize() {
    return maxRingSize;
  }

  @Override
  int choiceCount() {
    return choiceCount;
  }

  @Nullable
  @Override
  String edsServiceName() {
    return edsServiceName;
  }

  @Nullable
  @Override
  String dnsHostName() {
    return dnsHostName;
  }

  @Nullable
  @Override
  Bootstrapper.ServerInfo lrsServerInfo() {
    return lrsServerInfo;
  }

  @Nullable
  @Override
  Long maxConcurrentRequests() {
    return maxConcurrentRequests;
  }

  @Nullable
  @Override
  EnvoyServerProtoData.UpstreamTlsContext upstreamTlsContext() {
    return upstreamTlsContext;
  }

  @Nullable
  @Override
  ImmutableList<String> prioritizedClusterNames() {
    return prioritizedClusterNames;
  }

  @Nullable
  @Override
  EnvoyServerProtoData.OutlierDetection outlierDetection() {
    return outlierDetection;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof XdsClusterResource.CdsUpdate) {
      XdsClusterResource.CdsUpdate that = (XdsClusterResource.CdsUpdate) o;
      return this.clusterName.equals(that.clusterName())
          && this.clusterType.equals(that.clusterType())
          && this.lbPolicyConfig.equals(that.lbPolicyConfig())
          && this.minRingSize == that.minRingSize()
          && this.maxRingSize == that.maxRingSize()
          && this.choiceCount == that.choiceCount()
          && (this.edsServiceName == null ? that.edsServiceName() == null : this.edsServiceName.equals(that.edsServiceName()))
          && (this.dnsHostName == null ? that.dnsHostName() == null : this.dnsHostName.equals(that.dnsHostName()))
          && (this.lrsServerInfo == null ? that.lrsServerInfo() == null : this.lrsServerInfo.equals(that.lrsServerInfo()))
          && (this.maxConcurrentRequests == null ? that.maxConcurrentRequests() == null : this.maxConcurrentRequests.equals(that.maxConcurrentRequests()))
          && (this.upstreamTlsContext == null ? that.upstreamTlsContext() == null : this.upstreamTlsContext.equals(that.upstreamTlsContext()))
          && (this.prioritizedClusterNames == null ? that.prioritizedClusterNames() == null : this.prioritizedClusterNames.equals(that.prioritizedClusterNames()))
          && (this.outlierDetection == null ? that.outlierDetection() == null : this.outlierDetection.equals(that.outlierDetection()));
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

  static final class Builder extends XdsClusterResource.CdsUpdate.Builder {
    private String clusterName;
    private XdsClusterResource.CdsUpdate.ClusterType clusterType;
    private ImmutableMap<String, ?> lbPolicyConfig;
    private long minRingSize;
    private long maxRingSize;
    private int choiceCount;
    private String edsServiceName;
    private String dnsHostName;
    private Bootstrapper.ServerInfo lrsServerInfo;
    private Long maxConcurrentRequests;
    private EnvoyServerProtoData.UpstreamTlsContext upstreamTlsContext;
    private ImmutableList<String> prioritizedClusterNames;
    private EnvoyServerProtoData.OutlierDetection outlierDetection;
    private byte set$0;
    Builder() {
    }
    @Override
    protected XdsClusterResource.CdsUpdate.Builder clusterName(String clusterName) {
      if (clusterName == null) {
        throw new NullPointerException("Null clusterName");
      }
      this.clusterName = clusterName;
      return this;
    }
    @Override
    protected XdsClusterResource.CdsUpdate.Builder clusterType(XdsClusterResource.CdsUpdate.ClusterType clusterType) {
      if (clusterType == null) {
        throw new NullPointerException("Null clusterType");
      }
      this.clusterType = clusterType;
      return this;
    }
    @Override
    protected XdsClusterResource.CdsUpdate.Builder lbPolicyConfig(ImmutableMap<String, ?> lbPolicyConfig) {
      if (lbPolicyConfig == null) {
        throw new NullPointerException("Null lbPolicyConfig");
      }
      this.lbPolicyConfig = lbPolicyConfig;
      return this;
    }
    @Override
    protected XdsClusterResource.CdsUpdate.Builder minRingSize(long minRingSize) {
      this.minRingSize = minRingSize;
      set$0 |= (byte) 1;
      return this;
    }
    @Override
    protected XdsClusterResource.CdsUpdate.Builder maxRingSize(long maxRingSize) {
      this.maxRingSize = maxRingSize;
      set$0 |= (byte) 2;
      return this;
    }
    @Override
    protected XdsClusterResource.CdsUpdate.Builder choiceCount(int choiceCount) {
      this.choiceCount = choiceCount;
      set$0 |= (byte) 4;
      return this;
    }
    @Override
    protected XdsClusterResource.CdsUpdate.Builder edsServiceName(String edsServiceName) {
      this.edsServiceName = edsServiceName;
      return this;
    }
    @Override
    protected XdsClusterResource.CdsUpdate.Builder dnsHostName(String dnsHostName) {
      this.dnsHostName = dnsHostName;
      return this;
    }
    @Override
    protected XdsClusterResource.CdsUpdate.Builder lrsServerInfo(Bootstrapper.ServerInfo lrsServerInfo) {
      this.lrsServerInfo = lrsServerInfo;
      return this;
    }
    @Override
    protected XdsClusterResource.CdsUpdate.Builder maxConcurrentRequests(Long maxConcurrentRequests) {
      this.maxConcurrentRequests = maxConcurrentRequests;
      return this;
    }
    @Override
    protected XdsClusterResource.CdsUpdate.Builder upstreamTlsContext(EnvoyServerProtoData.UpstreamTlsContext upstreamTlsContext) {
      this.upstreamTlsContext = upstreamTlsContext;
      return this;
    }
    @Override
    protected XdsClusterResource.CdsUpdate.Builder prioritizedClusterNames(List<String> prioritizedClusterNames) {
      this.prioritizedClusterNames = (prioritizedClusterNames == null ? null : ImmutableList.copyOf(prioritizedClusterNames));
      return this;
    }
    @Override
    protected XdsClusterResource.CdsUpdate.Builder outlierDetection(EnvoyServerProtoData.OutlierDetection outlierDetection) {
      this.outlierDetection = outlierDetection;
      return this;
    }
    @Override
    XdsClusterResource.CdsUpdate build() {
      if (set$0 != 7
          || this.clusterName == null
          || this.clusterType == null
          || this.lbPolicyConfig == null) {
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
      return new AutoValue_XdsClusterResource_CdsUpdate(
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
