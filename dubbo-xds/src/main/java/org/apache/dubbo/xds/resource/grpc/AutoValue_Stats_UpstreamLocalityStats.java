package org.apache.dubbo.xds.resource.grpc;

import org.apache.dubbo.xds.resource.grpc.Stats.BackendLoadMetricStats;

import com.google.common.collect.ImmutableMap;

final class AutoValue_Stats_UpstreamLocalityStats extends Stats.UpstreamLocalityStats {

  private final Locality locality;

  private final long totalIssuedRequests;

  private final long totalSuccessfulRequests;

  private final long totalErrorRequests;

  private final long totalRequestsInProgress;

  private final ImmutableMap<String, BackendLoadMetricStats> loadMetricStatsMap;

  AutoValue_Stats_UpstreamLocalityStats(
      Locality locality,
      long totalIssuedRequests,
      long totalSuccessfulRequests,
      long totalErrorRequests,
      long totalRequestsInProgress,
      ImmutableMap<String, Stats.BackendLoadMetricStats> loadMetricStatsMap) {
    if (locality == null) {
      throw new NullPointerException("Null locality");
    }
    this.locality = locality;
    this.totalIssuedRequests = totalIssuedRequests;
    this.totalSuccessfulRequests = totalSuccessfulRequests;
    this.totalErrorRequests = totalErrorRequests;
    this.totalRequestsInProgress = totalRequestsInProgress;
    if (loadMetricStatsMap == null) {
      throw new NullPointerException("Null loadMetricStatsMap");
    }
    this.loadMetricStatsMap = loadMetricStatsMap;
  }

  @Override
  Locality locality() {
    return locality;
  }

  @Override
  long totalIssuedRequests() {
    return totalIssuedRequests;
  }

  @Override
  long totalSuccessfulRequests() {
    return totalSuccessfulRequests;
  }

  @Override
  long totalErrorRequests() {
    return totalErrorRequests;
  }

  @Override
  long totalRequestsInProgress() {
    return totalRequestsInProgress;
  }

  @Override
  ImmutableMap<String, Stats.BackendLoadMetricStats> loadMetricStatsMap() {
    return loadMetricStatsMap;
  }

  @Override
  public String toString() {
    return "UpstreamLocalityStats{"
        + "locality=" + locality + ", "
        + "totalIssuedRequests=" + totalIssuedRequests + ", "
        + "totalSuccessfulRequests=" + totalSuccessfulRequests + ", "
        + "totalErrorRequests=" + totalErrorRequests + ", "
        + "totalRequestsInProgress=" + totalRequestsInProgress + ", "
        + "loadMetricStatsMap=" + loadMetricStatsMap
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof Stats.UpstreamLocalityStats) {
      Stats.UpstreamLocalityStats that = (Stats.UpstreamLocalityStats) o;
      return this.locality.equals(that.locality())
          && this.totalIssuedRequests == that.totalIssuedRequests()
          && this.totalSuccessfulRequests == that.totalSuccessfulRequests()
          && this.totalErrorRequests == that.totalErrorRequests()
          && this.totalRequestsInProgress == that.totalRequestsInProgress()
          && this.loadMetricStatsMap.equals(that.loadMetricStatsMap());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= locality.hashCode();
    h$ *= 1000003;
    h$ ^= (int) ((totalIssuedRequests >>> 32) ^ totalIssuedRequests);
    h$ *= 1000003;
    h$ ^= (int) ((totalSuccessfulRequests >>> 32) ^ totalSuccessfulRequests);
    h$ *= 1000003;
    h$ ^= (int) ((totalErrorRequests >>> 32) ^ totalErrorRequests);
    h$ *= 1000003;
    h$ ^= (int) ((totalRequestsInProgress >>> 32) ^ totalRequestsInProgress);
    h$ *= 1000003;
    h$ ^= loadMetricStatsMap.hashCode();
    return h$;
  }

}
