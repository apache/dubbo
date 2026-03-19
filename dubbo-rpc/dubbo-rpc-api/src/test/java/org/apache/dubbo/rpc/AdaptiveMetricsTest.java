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
package org.apache.dubbo.rpc;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link AdaptiveMetrics}.
 *
 * <p>All tests drive through the public API only (no reflection, no Thread.sleep).
 * Verifies the fix for <a href="https://github.com/apache/dubbo/issues/15810">#15810</a>:
 * <ul>
 *   <li>Bug 1: penalty branch overwrites real RT on every normal response</li>
 *   <li>Bug 2: aggressive bit-shift decay collapses latency to zero</li>
 * </ul>
 */
class AdaptiveMetricsTest {

    private static final String FAST_KEY = "10.0.0.1:20880/com.example.TestService";
    private static final String SLOW_KEY = "10.0.0.2:20880/com.example.TestService";
    private static final String MEDIUM_KEY = "10.0.0.3:20880/com.example.TestService";
    private static final int WEIGHT = 100;
    private static final int TIMEOUT = 100;

    private Map<String, String> metricsMap(long curTime, long rt, String load) {
        Map<String, String> map = new HashMap<>();
        map.put("curTime", String.valueOf(curTime));
        map.put("rt", String.valueOf(rt));
        map.put("load", load);
        return map;
    }

    /**
     * Bug 1 regression: after setProviderMetrics, getLoad should use the real RT,
     * not overwrite it with timeout * 2. A fast server (10ms) must have strictly
     * lower load than a slow server (200ms).
     */
    @Test
    void freshMetrics_usesRealLatency_notPenalty() {
        AdaptiveMetrics am = new AdaptiveMetrics();
        long now = System.currentTimeMillis();

        // Fast server: 10ms RT
        am.addConsumerReq(FAST_KEY);
        am.setProviderMetrics(FAST_KEY, metricsMap(now, 10, "0.5"));
        am.addConsumerSuccess(FAST_KEY);

        double fastLoad = am.getLoad(FAST_KEY, WEIGHT, TIMEOUT);

        // Slow server: 200ms RT
        am.addConsumerReq(SLOW_KEY);
        am.setProviderMetrics(SLOW_KEY, metricsMap(now, 200, "0.5"));
        am.addConsumerSuccess(SLOW_KEY);

        double slowLoad = am.getLoad(SLOW_KEY, WEIGHT, TIMEOUT);

        // With the penalty bug, both would converge to timeout*2 → loads nearly equal
        assertTrue(fastLoad < slowLoad,
                "Fast server (10ms) should have lower load than slow server (200ms), "
                        + "got fast=" + fastLoad + " slow=" + slowLoad);
    }

    /**
     * Bug 2 regression: after multiple decay rounds without new provider metrics,
     * load must remain above zero. Decay-to-zero would make idle servers appear
     * "fastest" and attract all traffic.
     */
    @Test
    void decayWithoutUpdate_doesNotReachZero() {
        AdaptiveMetrics am = new AdaptiveMetrics();
        long now = System.currentTimeMillis();

        am.addConsumerReq(FAST_KEY);
        am.setProviderMetrics(FAST_KEY, metricsMap(now, 200, "0.5"));
        am.addConsumerSuccess(FAST_KEY);

        // First getLoad consumes the providerUpdated flag
        am.getLoad(FAST_KEY, WEIGHT, TIMEOUT);

        // Multiple getLoad calls without new metrics — all enter decay branch
        double load = 0;
        for (int i = 0; i < 50; i++) {
            load = am.getLoad(FAST_KEY, WEIGHT, TIMEOUT);
        }

        // With the bug: lastLatency >> large_multiple = 0, ewma → 0, load → 0
        assertTrue(load > 0, "Load should not decay to zero after idle period, got " + load);
    }

    /**
     * End-to-end: with 3 servers at different RTs, adaptive metrics should
     * consistently rank them correctly after multiple rounds.
     */
    @Test
    void multipleServers_fastServerPreferred() {
        AdaptiveMetrics am = new AdaptiveMetrics();

        // Simulate 20 rounds of traffic
        for (int round = 0; round < 20; round++) {
            long now = System.currentTimeMillis();

            am.addConsumerReq(FAST_KEY);
            am.setProviderMetrics(FAST_KEY, metricsMap(now, 10, "0.5"));
            am.addConsumerSuccess(FAST_KEY);

            am.addConsumerReq(MEDIUM_KEY);
            am.setProviderMetrics(MEDIUM_KEY, metricsMap(now, 50, "0.5"));
            am.addConsumerSuccess(MEDIUM_KEY);

            am.addConsumerReq(SLOW_KEY);
            am.setProviderMetrics(SLOW_KEY, metricsMap(now, 200, "0.5"));
            am.addConsumerSuccess(SLOW_KEY);
        }

        double loadFast = am.getLoad(FAST_KEY, WEIGHT, TIMEOUT);
        double loadMedium = am.getLoad(MEDIUM_KEY, WEIGHT, TIMEOUT);
        double loadSlow = am.getLoad(SLOW_KEY, WEIGHT, TIMEOUT);

        // Strict ordering: fast < medium < slow
        assertTrue(loadFast < loadMedium,
                "fast(" + loadFast + ") should be < medium(" + loadMedium + ")");
        assertTrue(loadMedium < loadSlow,
                "medium(" + loadMedium + ") should be < slow(" + loadSlow + ")");
    }

    /**
     * A server that degrades (RT jumps from 10ms to 500ms) should see its
     * load score increase, so the algorithm can shift traffic away.
     */
    @Test
    void degradedServer_loadIncreases() {
        AdaptiveMetrics am = new AdaptiveMetrics();

        // Warm up with fast responses
        for (int i = 0; i < 10; i++) {
            long now = System.currentTimeMillis();
            am.addConsumerReq(FAST_KEY);
            am.setProviderMetrics(FAST_KEY, metricsMap(now, 10, "0.5"));
            am.addConsumerSuccess(FAST_KEY);
        }
        double loadBefore = am.getLoad(FAST_KEY, WEIGHT, TIMEOUT);

        // Server degrades: RT jumps to 500ms, CPU load spikes
        for (int i = 0; i < 10; i++) {
            long now = System.currentTimeMillis();
            am.addConsumerReq(FAST_KEY);
            am.setProviderMetrics(FAST_KEY, metricsMap(now, 500, "0.9"));
            am.addConsumerSuccess(FAST_KEY);
        }
        double loadAfter = am.getLoad(FAST_KEY, WEIGHT, TIMEOUT);

        assertTrue(loadAfter > loadBefore,
                "Load should increase after degradation: before=" + loadBefore + " after=" + loadAfter);
    }

    /**
     * Guard: out-of-order serviceTime should be discarded by setProviderMetrics.
     * An older timestamp must not overwrite newer metrics.
     */
    @Test
    void outOfOrderServiceTime_isDiscarded() {
        AdaptiveMetrics am = new AdaptiveMetrics();
        long now = System.currentTimeMillis();

        // Set metrics with current time, RT=50ms
        am.addConsumerReq(FAST_KEY);
        am.setProviderMetrics(FAST_KEY, metricsMap(now, 50, "0.5"));
        am.addConsumerSuccess(FAST_KEY);
        double loadAfterFresh = am.getLoad(FAST_KEY, WEIGHT, TIMEOUT);

        // Attempt to set metrics with an older timestamp, RT=999ms
        am.addConsumerReq(FAST_KEY);
        am.setProviderMetrics(FAST_KEY, metricsMap(now - 10000, 999, "1.0"));
        am.addConsumerSuccess(FAST_KEY);
        double loadAfterStale = am.getLoad(FAST_KEY, WEIGHT, TIMEOUT);

        // The stale update should have been discarded — load should not spike
        // (if the stale 999ms RT were accepted, loadAfterStale would be much higher)
        assertTrue(loadAfterStale <= loadAfterFresh,
                "Stale metrics should be discarded: loadAfterFresh=" + loadAfterFresh
                        + " loadAfterStale=" + loadAfterStale);
    }

    /**
     * Guard: when pickTime exceeds timeout * 2, getLoad must return 0 (forced pick)
     * to prevent starvation. This behavior must not be broken by the fix.
     */
    @Test
    void forcedPick_notBroken() {
        AdaptiveMetrics am = new AdaptiveMetrics();
        long now = System.currentTimeMillis();

        am.addConsumerReq(FAST_KEY);
        am.setProviderMetrics(FAST_KEY, metricsMap(now, 50, "0.5"));
        am.addConsumerSuccess(FAST_KEY);

        // Set pickTime far in the past → triggers forced pick (return 0)
        am.setPickTime(FAST_KEY, now - TIMEOUT * 3);

        double load = am.getLoad(FAST_KEY, WEIGHT, TIMEOUT);
        assertEquals(0.0, load, "getLoad should return 0 when pickTime exceeds timeout * 2");
    }
}
