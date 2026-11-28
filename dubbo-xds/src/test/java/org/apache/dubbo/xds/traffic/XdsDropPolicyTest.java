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

import java.util.concurrent.atomic.AtomicInteger;

import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment.Policy;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment.Policy.DropOverload;
import io.grpc.xds.shaded.io.envoyproxy.envoy.type.v3.FractionalPercent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class XdsDropPolicyTest {
    @Test
    void orderedIndependentSamplingUsesTheFirstMatchingCategory() {
        ClusterLoadAssignment assignment = assignment(
                drop("first", 25, FractionalPercent.DenominatorType.HUNDRED),
                drop("second", 50, FractionalPercent.DenominatorType.HUNDRED));
        AtomicInteger samples = new AtomicInteger();
        assertEquals("second", XdsDropPolicy.category(assignment, bound -> {
            assertEquals(100, bound);
            return samples.incrementAndGet() == 1 ? 30 : 40;
        }));
        assertEquals(2, samples.get());
        assertEquals("first", XdsDropPolicy.category(assignment, bound -> 0));
        assertNull(XdsDropPolicy.category(assignment, bound -> 99));
    }

    @Test
    void unsignedNumeratorsClampAndZeroDoesNotConsumeRandomness() {
        for (FractionalPercent.DenominatorType denominator : new FractionalPercent.DenominatorType[] {
            FractionalPercent.DenominatorType.HUNDRED,
            FractionalPercent.DenominatorType.TEN_THOUSAND,
            FractionalPercent.DenominatorType.MILLION
        }) {
            ClusterLoadAssignment assignment = assignment(drop("zero", 0, denominator), drop("all", -1, denominator));
            assertEquals("all", XdsDropPolicy.category(assignment, bound -> {
                fail("Zero and 100% drops do not need randomness");
                return 0;
            }));
        }
    }

    @Test
    void malformedPoliciesRejectBeforeAcceptance() {
        assertThrows(
                IllegalArgumentException.class,
                () -> XdsDropPolicy.validate(assignment(drop("", 1, FractionalPercent.DenominatorType.HUNDRED))));
        DropOverload invalid = DropOverload.newBuilder()
                .setCategory("bad")
                .setDropPercentage(
                        FractionalPercent.newBuilder().setNumerator(1).setDenominatorValue(10))
                .build();
        assertThrows(IllegalArgumentException.class, () -> XdsDropPolicy.validate(assignment(invalid)));
    }

    private static DropOverload drop(String category, int numerator, FractionalPercent.DenominatorType denominator) {
        return DropOverload.newBuilder()
                .setCategory(category)
                .setDropPercentage(
                        FractionalPercent.newBuilder().setNumerator(numerator).setDenominator(denominator))
                .build();
    }

    private static ClusterLoadAssignment assignment(DropOverload... drops) {
        Policy.Builder policy = Policy.newBuilder();
        for (DropOverload drop : drops) policy.addDropOverloads(drop);
        return ClusterLoadAssignment.newBuilder()
                .setClusterName("cluster")
                .setPolicy(policy)
                .build();
    }
}
