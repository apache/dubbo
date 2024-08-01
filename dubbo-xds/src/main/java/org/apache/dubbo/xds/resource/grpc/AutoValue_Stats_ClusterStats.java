package org.apache.dubbo.xds.resource.grpc;

import org.apache.dubbo.common.lang.Nullable;
import org.apache.dubbo.xds.resource.grpc.Stats.UpstreamLocalityStats;

import com.google.common.collect.ImmutableList;

final class AutoValue_Stats_ClusterStats extends Stats.ClusterStats {

  private final String clusterName;

  @Nullable
  private final String clusterServiceName;

  private final ImmutableList<UpstreamLocalityStats> upstreamLocalityStatsList;

  private final ImmutableList<Stats.DroppedRequests> droppedRequestsList;

  private final long totalDroppedRequests;

  private final long loadReportIntervalNano;

  private AutoValue_Stats_ClusterStats(
      String clusterName,
      @Nullable String clusterServiceName,
      ImmutableList<Stats.UpstreamLocalityStats> upstreamLocalityStatsList,
      ImmutableList<Stats.DroppedRequests> droppedRequestsList,
      long totalDroppedRequests,
      long loadReportIntervalNano) {
    this.clusterName = clusterName;
    this.clusterServiceName = clusterServiceName;
    this.upstreamLocalityStatsList = upstreamLocalityStatsList;
    this.droppedRequestsList = droppedRequestsList;
    this.totalDroppedRequests = totalDroppedRequests;
    this.loadReportIntervalNano = loadReportIntervalNano;
  }

  @Override
  String clusterName() {
    return clusterName;
  }

  @Nullable
  @Override
  String clusterServiceName() {
    return clusterServiceName;
  }

  @Override
  ImmutableList<Stats.UpstreamLocalityStats> upstreamLocalityStatsList() {
    return upstreamLocalityStatsList;
  }

  @Override
  ImmutableList<Stats.DroppedRequests> droppedRequestsList() {
    return droppedRequestsList;
  }

  @Override
  long totalDroppedRequests() {
    return totalDroppedRequests;
  }

  @Override
  long loadReportIntervalNano() {
    return loadReportIntervalNano;
  }

  @Override
  public String toString() {
    return "ClusterStats{"
        + "clusterName=" + clusterName + ", "
        + "clusterServiceName=" + clusterServiceName + ", "
        + "upstreamLocalityStatsList=" + upstreamLocalityStatsList + ", "
        + "droppedRequestsList=" + droppedRequestsList + ", "
        + "totalDroppedRequests=" + totalDroppedRequests + ", "
        + "loadReportIntervalNano=" + loadReportIntervalNano
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof Stats.ClusterStats) {
      Stats.ClusterStats that = (Stats.ClusterStats) o;
      return this.clusterName.equals(that.clusterName())
          && (this.clusterServiceName == null ? that.clusterServiceName() == null : this.clusterServiceName.equals(that.clusterServiceName()))
          && this.upstreamLocalityStatsList.equals(that.upstreamLocalityStatsList())
          && this.droppedRequestsList.equals(that.droppedRequestsList())
          && this.totalDroppedRequests == that.totalDroppedRequests()
          && this.loadReportIntervalNano == that.loadReportIntervalNano();
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= clusterName.hashCode();
    h$ *= 1000003;
    h$ ^= (clusterServiceName == null) ? 0 : clusterServiceName.hashCode();
    h$ *= 1000003;
    h$ ^= upstreamLocalityStatsList.hashCode();
    h$ *= 1000003;
    h$ ^= droppedRequestsList.hashCode();
    h$ *= 1000003;
    h$ ^= (int) ((totalDroppedRequests >>> 32) ^ totalDroppedRequests);
    h$ *= 1000003;
    h$ ^= (int) ((loadReportIntervalNano >>> 32) ^ loadReportIntervalNano);
    return h$;
  }

  static final class Builder extends Stats.ClusterStats.Builder {
    private String clusterName;
    private String clusterServiceName;
    private ImmutableList.Builder<Stats.UpstreamLocalityStats> upstreamLocalityStatsListBuilder$;
    private ImmutableList<Stats.UpstreamLocalityStats> upstreamLocalityStatsList;
    private ImmutableList.Builder<Stats.DroppedRequests> droppedRequestsListBuilder$;
    private ImmutableList<Stats.DroppedRequests> droppedRequestsList;
    private long totalDroppedRequests;
    private long loadReportIntervalNano;
    private byte set$0;
    Builder() {
    }
    @Override
    Stats.ClusterStats.Builder clusterName(String clusterName) {
      if (clusterName == null) {
        throw new NullPointerException("Null clusterName");
      }
      this.clusterName = clusterName;
      return this;
    }
    @Override
    Stats.ClusterStats.Builder clusterServiceName(String clusterServiceName) {
      this.clusterServiceName = clusterServiceName;
      return this;
    }
    @Override
    ImmutableList.Builder<Stats.UpstreamLocalityStats> upstreamLocalityStatsListBuilder() {
      if (upstreamLocalityStatsListBuilder$ == null) {
        upstreamLocalityStatsListBuilder$ = ImmutableList.builder();
      }
      return upstreamLocalityStatsListBuilder$;
    }
    @Override
    ImmutableList.Builder<Stats.DroppedRequests> droppedRequestsListBuilder() {
      if (droppedRequestsListBuilder$ == null) {
        droppedRequestsListBuilder$ = ImmutableList.builder();
      }
      return droppedRequestsListBuilder$;
    }
    @Override
    Stats.ClusterStats.Builder totalDroppedRequests(long totalDroppedRequests) {
      this.totalDroppedRequests = totalDroppedRequests;
      set$0 |= (byte) 1;
      return this;
    }
    @Override
    Stats.ClusterStats.Builder loadReportIntervalNano(long loadReportIntervalNano) {
      this.loadReportIntervalNano = loadReportIntervalNano;
      set$0 |= (byte) 2;
      return this;
    }
    @Override
    long loadReportIntervalNano() {
      if ((set$0 & 2) == 0) {
        throw new IllegalStateException("Property \"loadReportIntervalNano\" has not been set");
      }
      return loadReportIntervalNano;
    }
    @Override
    Stats.ClusterStats build() {
      if (upstreamLocalityStatsListBuilder$ != null) {
        this.upstreamLocalityStatsList = upstreamLocalityStatsListBuilder$.build();
      } else if (this.upstreamLocalityStatsList == null) {
        this.upstreamLocalityStatsList = ImmutableList.of();
      }
      if (droppedRequestsListBuilder$ != null) {
        this.droppedRequestsList = droppedRequestsListBuilder$.build();
      } else if (this.droppedRequestsList == null) {
        this.droppedRequestsList = ImmutableList.of();
      }
      if (set$0 != 3
          || this.clusterName == null) {
        StringBuilder missing = new StringBuilder();
        if (this.clusterName == null) {
          missing.append(" clusterName");
        }
        if ((set$0 & 1) == 0) {
          missing.append(" totalDroppedRequests");
        }
        if ((set$0 & 2) == 0) {
          missing.append(" loadReportIntervalNano");
        }
        throw new IllegalStateException("Missing required properties:" + missing);
      }
      return new AutoValue_Stats_ClusterStats(
          this.clusterName,
          this.clusterServiceName,
          this.upstreamLocalityStatsList,
          this.droppedRequestsList,
          this.totalDroppedRequests,
          this.loadReportIntervalNano);
    }
  }

}
