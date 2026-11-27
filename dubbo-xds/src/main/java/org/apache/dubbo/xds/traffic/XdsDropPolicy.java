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
package org.apache.dubbo.xds.traffic;

import org.apache.dubbo.xds.client.XdsResourceGraph;
import org.apache.dubbo.xds.client.XdsResourceGraph.Snapshot;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntUnaryOperator;

import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment.Policy.DropOverload;
import io.grpc.xds.shaded.io.envoyproxy.envoy.type.v3.FractionalPercent;

/** A27 ordered independent EDS drops; sampled once per attempt before connection waiting. */
public final class XdsDropPolicy {
    private XdsDropPolicy() {}

    public static void validate(ClusterLoadAssignment assignment) {
        for (DropOverload drop : assignment.getPolicy().getDropOverloadsList()) {
            if (drop.getCategory().isEmpty()) throw new IllegalArgumentException("EDS drop category is empty");
            denominator(drop.getDropPercentage());
        }
    }

    private static int denominator(FractionalPercent fraction) {
        switch (fraction.getDenominator()) {
            case HUNDRED:
                return 100;
            case TEN_THOUSAND:
                return 10_000;
            case MILLION:
                return 1_000_000;
            default:
                throw new IllegalArgumentException("Unsupported EDS drop denominator");
        }
    }

    static String category(ClusterLoadAssignment assignment, IntUnaryOperator random) {
        for (DropOverload drop : assignment.getPolicy().getDropOverloadsList()) {
            FractionalPercent fraction = drop.getDropPercentage();
            int denominator = denominator(fraction);
            long numerator = Integer.toUnsignedLong(fraction.getNumerator());
            if (numerator >= denominator || (numerator > 0 && random.applyAsInt(denominator) < numerator)) {
                return drop.getCategory();
            }
        }
        return null;
    }

    public static void apply(Snapshot snapshot, String clusterName) {
        Cluster cluster = snapshot == null ? null : snapshot.getClusters().get(clusterName);
        ClusterLoadAssignment assignment =
                cluster == null ? null : snapshot.getEndpoints().get(XdsResourceGraph.edsName(cluster));
        if (assignment == null) return;
        String category =
                category(assignment, bound -> ThreadLocalRandom.current().nextInt(bound));
        if (category != null) {
            snapshot.getLoadStats().edsDropped(cluster, category);
            throw XdsCircuitBreaker.DropException.eds();
        }
    }
}
