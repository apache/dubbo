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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nullable;

import java.util.Map;

/** Represents client load stats. */
final class Stats {
  private Stats() {}

  /** Cluster-level load stats. */
  @AutoValue
  abstract static class ClusterStats {
    abstract String clusterName();

    @Nullable
    abstract String clusterServiceName();

    abstract ImmutableList<UpstreamLocalityStats> upstreamLocalityStatsList();

    abstract ImmutableList<DroppedRequests> droppedRequestsList();

    abstract long totalDroppedRequests();

    abstract long loadReportIntervalNano();

    static Builder newBuilder() {
      return new AutoValue_Stats_ClusterStats.Builder()
          .totalDroppedRequests(0L)  // default initialization
          .loadReportIntervalNano(0L);
    }

    @AutoValue.Builder
    abstract static class Builder {
      abstract Builder clusterName(String clusterName);

      abstract Builder clusterServiceName(String clusterServiceName);

      abstract ImmutableList.Builder<UpstreamLocalityStats> upstreamLocalityStatsListBuilder();

      Builder addUpstreamLocalityStats(UpstreamLocalityStats upstreamLocalityStats) {
        upstreamLocalityStatsListBuilder().add(upstreamLocalityStats);
        return this;
      }

      abstract ImmutableList.Builder<DroppedRequests> droppedRequestsListBuilder();

      Builder addDroppedRequests(DroppedRequests droppedRequests) {
        droppedRequestsListBuilder().add(droppedRequests);
        return this;
      }

      abstract Builder totalDroppedRequests(long totalDroppedRequests);

      abstract Builder loadReportIntervalNano(long loadReportIntervalNano);

      abstract long loadReportIntervalNano();

      abstract ClusterStats build();
    }
  }

  /** Stats for dropped requests. */
  @AutoValue
  abstract static class DroppedRequests {
    abstract String category();

    abstract long droppedCount();

    static DroppedRequests create(String category, long droppedCount) {
      return new AutoValue_Stats_DroppedRequests(category, droppedCount);
    }
  }

  /** Load stats aggregated in locality level. */
  @AutoValue
  abstract static class UpstreamLocalityStats {
    abstract Locality locality();

    abstract long totalIssuedRequests();

    abstract long totalSuccessfulRequests();

    abstract long totalErrorRequests();

    abstract long totalRequestsInProgress();

    abstract ImmutableMap<String, BackendLoadMetricStats> loadMetricStatsMap();

    static UpstreamLocalityStats create(Locality locality, long totalIssuedRequests,
        long totalSuccessfulRequests, long totalErrorRequests, long totalRequestsInProgress,
        Map<String, BackendLoadMetricStats> loadMetricStatsMap) {
      return new AutoValue_Stats_UpstreamLocalityStats(locality, totalIssuedRequests,
          totalSuccessfulRequests, totalErrorRequests, totalRequestsInProgress,
          ImmutableMap.copyOf(loadMetricStatsMap));
    }
  }

  /**
   * Load metric stats for multi-dimensional load balancing.
   */
  static final class BackendLoadMetricStats {

    private long numRequestsFinishedWithMetric;
    private double totalMetricValue;

    BackendLoadMetricStats(long numRequestsFinishedWithMetric, double totalMetricValue) {
      this.numRequestsFinishedWithMetric = numRequestsFinishedWithMetric;
      this.totalMetricValue = totalMetricValue;
    }

    public long numRequestsFinishedWithMetric() {
      return numRequestsFinishedWithMetric;
    }

    public double totalMetricValue() {
      return totalMetricValue;
    }

    /**
     * Adds the given {@code metricValue} and increments the number of requests finished counter for
     * the existing {@link BackendLoadMetricStats}.
     */
    public void addMetricValueAndIncrementRequestsFinished(double metricValue) {
      numRequestsFinishedWithMetric += 1;
      totalMetricValue += metricValue;
    }
  }
}
