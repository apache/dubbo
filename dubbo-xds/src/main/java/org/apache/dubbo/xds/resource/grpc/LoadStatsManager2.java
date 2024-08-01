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

import org.apache.dubbo.xds.resource.grpc.Stats.BackendLoadMetricStats;
import org.apache.dubbo.xds.resource.grpc.Stats.ClusterStats;
import org.apache.dubbo.xds.resource.grpc.Stats.DroppedRequests;
import org.apache.dubbo.xds.resource.grpc.Stats.UpstreamLocalityStats;

import com.google.common.base.Stopwatch;
import com.google.common.base.Supplier;
import com.google.common.collect.Sets;
import io.grpc.Status;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Manages client side traffic stats. Drop stats are maintained in cluster (with edsServiceName)
 * granularity and load stats (request counts) are maintained in locality granularity.
 */
@ThreadSafe
final class LoadStatsManager2 {
  // Recorders for drops of each cluster:edsServiceName.
  private final Map<String, Map<String, ReferenceCounted<ClusterDropStats>>> allDropStats =
      new HashMap<>();
  // Recorders for loads of each cluster:edsServiceName:locality.
  private final Map<String, Map<String,
      Map<Locality, ReferenceCounted<ClusterLocalityStats>>>> allLoadStats = new HashMap<>();
  private final Supplier<Stopwatch> stopwatchSupplier;

  LoadStatsManager2(Supplier<Stopwatch> stopwatchSupplier) {
    this.stopwatchSupplier = checkNotNull(stopwatchSupplier, "stopwatchSupplier");
  }

  /**
   * Gets or creates the stats object for recording drops for the specified cluster with
   * edsServiceName. The returned object is reference counted and the caller should use {@link
   * ClusterDropStats#release()} to release its <i>hard</i> reference when it is safe to discard
   * future stats for the cluster.
   */
  synchronized ClusterDropStats getClusterDropStats(
      String cluster, @Nullable String edsServiceName) {
    if (!allDropStats.containsKey(cluster)) {
      allDropStats.put(cluster, new HashMap<String, ReferenceCounted<ClusterDropStats>>());
    }
    Map<String, ReferenceCounted<ClusterDropStats>> perClusterCounters = allDropStats.get(cluster);
    if (!perClusterCounters.containsKey(edsServiceName)) {
      perClusterCounters.put(
          edsServiceName,
          ReferenceCounted.wrap(new ClusterDropStats(
              cluster, edsServiceName, stopwatchSupplier.get())));
    }
    ReferenceCounted<ClusterDropStats> ref = perClusterCounters.get(edsServiceName);
    ref.retain();
    return ref.get();
  }

  private synchronized void releaseClusterDropCounter(
      String cluster, @Nullable String edsServiceName) {
    checkState(allDropStats.containsKey(cluster)
            && allDropStats.get(cluster).containsKey(edsServiceName),
        "stats for cluster %s, edsServiceName %s not exits", cluster, edsServiceName);
    ReferenceCounted<ClusterDropStats> ref = allDropStats.get(cluster).get(edsServiceName);
    ref.release();
  }

  /**
   * Gets or creates the stats object for recording loads for the specified locality (in the
   * specified cluster with edsServiceName). The returned object is reference counted and the
   * caller should use {@link ClusterLocalityStats#release} to release its <i>hard</i> reference
   * when it is safe to discard the future stats for the locality.
   */
  synchronized ClusterLocalityStats getClusterLocalityStats(
      String cluster, @Nullable String edsServiceName, Locality locality) {
    if (!allLoadStats.containsKey(cluster)) {
      allLoadStats.put(
          cluster,
          new HashMap<String, Map<Locality, ReferenceCounted<ClusterLocalityStats>>>());
    }
    Map<String, Map<Locality, ReferenceCounted<ClusterLocalityStats>>> perClusterCounters =
        allLoadStats.get(cluster);
    if (!perClusterCounters.containsKey(edsServiceName)) {
      perClusterCounters.put(
          edsServiceName, new HashMap<Locality, ReferenceCounted<ClusterLocalityStats>>());
    }
    Map<Locality, ReferenceCounted<ClusterLocalityStats>> localityStats =
        perClusterCounters.get(edsServiceName);
    if (!localityStats.containsKey(locality)) {
      localityStats.put(
          locality,
          ReferenceCounted.wrap(new ClusterLocalityStats(
              cluster, edsServiceName, locality, stopwatchSupplier.get())));
    }
    ReferenceCounted<ClusterLocalityStats> ref = localityStats.get(locality);
    ref.retain();
    return ref.get();
  }

  private synchronized void releaseClusterLocalityLoadCounter(
      String cluster, @Nullable String edsServiceName, Locality locality) {
    checkState(allLoadStats.containsKey(cluster)
            && allLoadStats.get(cluster).containsKey(edsServiceName)
            && allLoadStats.get(cluster).get(edsServiceName).containsKey(locality),
        "stats for cluster %s, edsServiceName %s, locality %s not exits",
        cluster, edsServiceName, locality);
    ReferenceCounted<ClusterLocalityStats> ref =
        allLoadStats.get(cluster).get(edsServiceName).get(locality);
    ref.release();
  }

  /**
   * Gets the traffic stats (drops and loads) as a list of {@link ClusterStats} recorded for the
   * specified cluster since the previous call of this method or {@link
   * #getAllClusterStatsReports}. A {@link ClusterStats} includes stats for a specific cluster with
   * edsServiceName.
   */
  synchronized List<ClusterStats> getClusterStatsReports(String cluster) {
    if (!allDropStats.containsKey(cluster) && !allLoadStats.containsKey(cluster)) {
      return Collections.emptyList();
    }
    Map<String, ReferenceCounted<ClusterDropStats>> clusterDropStats = allDropStats.get(cluster);
    Map<String, Map<Locality, ReferenceCounted<ClusterLocalityStats>>> clusterLoadStats =
        allLoadStats.get(cluster);
    Map<String, ClusterStats.Builder> statsReportBuilders = new HashMap<>();
    // Populate drop stats.
    if (clusterDropStats != null) {
      Set<String> toDiscard = new HashSet<>();
      for (String edsServiceName : clusterDropStats.keySet()) {
        ClusterStats.Builder builder = ClusterStats.newBuilder().clusterName(cluster);
        if (edsServiceName != null) {
          builder.clusterServiceName(edsServiceName);
        }
        ReferenceCounted<ClusterDropStats> ref = clusterDropStats.get(edsServiceName);
        if (ref.getReferenceCount() == 0) {  // stats object no longer needed after snapshot
          toDiscard.add(edsServiceName);
        }
        ClusterDropStatsSnapshot dropStatsSnapshot = ref.get().snapshot();
        long totalCategorizedDrops = 0L;
        for (Map.Entry<String, Long> entry : dropStatsSnapshot.categorizedDrops.entrySet()) {
          builder.addDroppedRequests(DroppedRequests.create(entry.getKey(), entry.getValue()));
          totalCategorizedDrops += entry.getValue();
        }
        builder.totalDroppedRequests(
            totalCategorizedDrops + dropStatsSnapshot.uncategorizedDrops);
        builder.loadReportIntervalNano(dropStatsSnapshot.durationNano);
        statsReportBuilders.put(edsServiceName, builder);
      }
      clusterDropStats.keySet().removeAll(toDiscard);
    }
    // Populate load stats for all localities in the cluster.
    if (clusterLoadStats != null) {
      Set<String> toDiscard = new HashSet<>();
      for (String edsServiceName : clusterLoadStats.keySet()) {
        ClusterStats.Builder builder = statsReportBuilders.get(edsServiceName);
        if (builder == null) {
          builder = ClusterStats.newBuilder().clusterName(cluster);
          if (edsServiceName != null) {
            builder.clusterServiceName(edsServiceName);
          }
          statsReportBuilders.put(edsServiceName, builder);
        }
        Map<Locality, ReferenceCounted<ClusterLocalityStats>> localityStats =
            clusterLoadStats.get(edsServiceName);
        Set<Locality> localitiesToDiscard = new HashSet<>();
        for (Locality locality : localityStats.keySet()) {
          ReferenceCounted<ClusterLocalityStats> ref = localityStats.get(locality);
          ClusterLocalityStatsSnapshot snapshot = ref.get().snapshot();
          // Only discard stats object after all in-flight calls under recording had finished.
          if (ref.getReferenceCount() == 0 && snapshot.callsInProgress == 0) {
            localitiesToDiscard.add(locality);
          }
          UpstreamLocalityStats upstreamLocalityStats = UpstreamLocalityStats.create(
              locality, snapshot.callsIssued, snapshot.callsSucceeded, snapshot.callsFailed,
              snapshot.callsInProgress, snapshot.loadMetricStatsMap);
          builder.addUpstreamLocalityStats(upstreamLocalityStats);
          // Use the max (drops/loads) recording interval as the overall interval for the
          // cluster's stats. In general, they should be mostly identical.
          builder.loadReportIntervalNano(
              Math.max(builder.loadReportIntervalNano(), snapshot.durationNano));
        }
        localityStats.keySet().removeAll(localitiesToDiscard);
        if (localityStats.isEmpty()) {
          toDiscard.add(edsServiceName);
        }
      }
      clusterLoadStats.keySet().removeAll(toDiscard);
    }
    List<ClusterStats> res = new ArrayList<>();
    for (ClusterStats.Builder builder : statsReportBuilders.values()) {
      res.add(builder.build());
    }
    return Collections.unmodifiableList(res);
  }

  /**
   * Gets the traffic stats (drops and loads) as a list of {@link ClusterStats} recorded for all
   * clusters since the previous call of this method or {@link #getClusterStatsReports} for each
   * specific cluster. A {@link ClusterStats} includes stats for a specific cluster with
   * edsServiceName.
   */
  synchronized List<ClusterStats> getAllClusterStatsReports() {
    Set<String> allClusters = Sets.union(allDropStats.keySet(), allLoadStats.keySet());
    List<ClusterStats> res = new ArrayList<>();
    for (String cluster : allClusters) {
      res.addAll(getClusterStatsReports(cluster));
    }
    return Collections.unmodifiableList(res);
  }

  /**
   * Recorder for dropped requests. One instance per cluster with edsServiceName.
   */
  @ThreadSafe
  final class ClusterDropStats {
    private final String clusterName;
    @Nullable
    private final String edsServiceName;
    private final AtomicLong uncategorizedDrops = new AtomicLong();
    private final ConcurrentMap<String, AtomicLong> categorizedDrops = new ConcurrentHashMap<>();
    private final Stopwatch stopwatch;

    private ClusterDropStats(
        String clusterName, @Nullable String edsServiceName, Stopwatch stopwatch) {
      this.clusterName = checkNotNull(clusterName, "clusterName");
      this.edsServiceName = edsServiceName;
      this.stopwatch = checkNotNull(stopwatch, "stopwatch");
      stopwatch.reset().start();
    }

    /**
     * Records a dropped request with the specified category.
     */
    void recordDroppedRequest(String category) {
      // There is a race between this method and snapshot(), causing one drop recorded but may not
      // be included in any snapshot. This is acceptable and the race window is extremely small.
      AtomicLong counter = categorizedDrops.putIfAbsent(category, new AtomicLong(1L));
      if (counter != null) {
        counter.getAndIncrement();
      }
    }

    /**
     * Records a dropped request without category.
     */
    void recordDroppedRequest() {
      uncategorizedDrops.getAndIncrement();
    }

    /**
     * Release the <i>hard</i> reference for this stats object (previously obtained via {@link
     * LoadStatsManager2#getClusterDropStats}). The object may still be recording
     * drops after this method, but there is no guarantee drops recorded after this point will
     * be included in load reports.
     */
    void release() {
      LoadStatsManager2.this.releaseClusterDropCounter(clusterName, edsServiceName);
    }

    private ClusterDropStatsSnapshot snapshot() {
      Map<String, Long> drops = new HashMap<>();
      for (Map.Entry<String, AtomicLong> entry : categorizedDrops.entrySet()) {
        drops.put(entry.getKey(), entry.getValue().get());
      }
      categorizedDrops.clear();
      long duration = stopwatch.elapsed(TimeUnit.NANOSECONDS);
      stopwatch.reset().start();
      return new ClusterDropStatsSnapshot(drops, uncategorizedDrops.getAndSet(0), duration);
    }
  }

  private static final class ClusterDropStatsSnapshot {
    private final Map<String, Long> categorizedDrops;
    private final long uncategorizedDrops;
    private final long durationNano;

    private ClusterDropStatsSnapshot(
        Map<String, Long> categorizedDrops, long uncategorizedDrops, long durationNano) {
      this.categorizedDrops = Collections.unmodifiableMap(
          checkNotNull(categorizedDrops, "categorizedDrops"));
      this.uncategorizedDrops = uncategorizedDrops;
      this.durationNano = durationNano;
    }
  }

  /**
   * Recorder for client loads. One instance per locality (in cluster with edsService).
   */
  @ThreadSafe
  final class ClusterLocalityStats {
    private final String clusterName;
    @Nullable
    private final String edsServiceName;
    private final Locality locality;
    private final Stopwatch stopwatch;
    private final AtomicLong callsInProgress = new AtomicLong();
    private final AtomicLong callsSucceeded = new AtomicLong();
    private final AtomicLong callsFailed = new AtomicLong();
    private final AtomicLong callsIssued = new AtomicLong();
    private Map<String, BackendLoadMetricStats> loadMetricStatsMap = new HashMap<>();

    private ClusterLocalityStats(
        String clusterName, @Nullable String edsServiceName, Locality locality,
        Stopwatch stopwatch) {
      this.clusterName = checkNotNull(clusterName, "clusterName");
      this.edsServiceName = edsServiceName;
      this.locality = checkNotNull(locality, "locality");
      this.stopwatch = checkNotNull(stopwatch, "stopwatch");
      stopwatch.reset().start();
    }

    /**
     * Records a request being issued.
     */
    void recordCallStarted() {
      callsIssued.getAndIncrement();
      callsInProgress.getAndIncrement();
    }

    /**
     * Records a request finished with the given status.
     */
    void recordCallFinished(Status status) {
      callsInProgress.getAndDecrement();
      if (status.isOk()) {
        callsSucceeded.getAndIncrement();
      } else {
        callsFailed.getAndIncrement();
      }
    }

    /**
     * Records all custom named backend load metric stats for per-call load reporting. For each
     * metric key {@code name}, creates a new {@link BackendLoadMetricStats} with a finished
     * requests counter of 1 and the {@code value} if the key is not present in the map. Otherwise,
     * increments the finished requests counter and adds the {@code value} to the existing
     * {@link BackendLoadMetricStats}.
     */
    synchronized void recordBackendLoadMetricStats(Map<String, Double> namedMetrics) {
      namedMetrics.forEach((name, value) -> {
        if (!loadMetricStatsMap.containsKey(name)) {
          loadMetricStatsMap.put(name, new BackendLoadMetricStats(1, value));
        } else {
          loadMetricStatsMap.get(name).addMetricValueAndIncrementRequestsFinished(value);
        }
      });
    }

    /**
     * Release the <i>hard</i> reference for this stats object (previously obtained via {@link
     * LoadStatsManager2#getClusterLocalityStats}). The object may still be
     * recording loads after this method, but there is no guarantee loads recorded after this
     * point will be included in load reports.
     */
    void release() {
      LoadStatsManager2.this.releaseClusterLocalityLoadCounter(
          clusterName, edsServiceName, locality);
    }

    private ClusterLocalityStatsSnapshot snapshot() {
      long duration = stopwatch.elapsed(TimeUnit.NANOSECONDS);
      stopwatch.reset().start();
      Map<String, BackendLoadMetricStats> loadMetricStatsMapCopy;
      synchronized (this) {
        loadMetricStatsMapCopy = Collections.unmodifiableMap(loadMetricStatsMap);
        loadMetricStatsMap = new HashMap<>();
      }
      return new ClusterLocalityStatsSnapshot(callsSucceeded.getAndSet(0), callsInProgress.get(),
          callsFailed.getAndSet(0), callsIssued.getAndSet(0), duration, loadMetricStatsMapCopy);
    }
  }

  private static final class ClusterLocalityStatsSnapshot {
    private final long callsSucceeded;
    private final long callsInProgress;
    private final long callsFailed;
    private final long callsIssued;
    private final long durationNano;
    private final Map<String, BackendLoadMetricStats> loadMetricStatsMap;

    private ClusterLocalityStatsSnapshot(
        long callsSucceeded, long callsInProgress, long callsFailed, long callsIssued,
        long durationNano, Map<String, BackendLoadMetricStats> loadMetricStatsMap) {
      this.callsSucceeded = callsSucceeded;
      this.callsInProgress = callsInProgress;
      this.callsFailed = callsFailed;
      this.callsIssued = callsIssued;
      this.durationNano = durationNano;
      this.loadMetricStatsMap = Collections.unmodifiableMap(
          checkNotNull(loadMetricStatsMap, "loadMetricStatsMap"));
    }
  }
}
