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

import org.apache.dubbo.rpc.TriRpcStatus;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RetryPolicy;

/** gRFC A44 converts only gRPC conditions; proxy retry predicates and per-try timers are ignored. */
public final class XdsRetryPolicy {
    private final int maxAttempts;
    private final Set<TriRpcStatus.Code> codes;
    private final long initialBackoff;
    private final long maximumBackoff;

    private XdsRetryPolicy(int maxAttempts, Set<TriRpcStatus.Code> codes, long initial, long maximum) {
        this.maxAttempts = maxAttempts;
        this.codes = Collections.unmodifiableSet(codes);
        this.initialBackoff = initial;
        this.maximumBackoff = maximum;
    }

    public static XdsRetryPolicy parse(RetryPolicy policy) {
        long retries = policy.hasNumRetries()
                ? Integer.toUnsignedLong(policy.getNumRetries().getValue())
                : 1;
        if (retries == 0) {
            throw new IllegalArgumentException("xDS num_retries must be positive");
        }
        long initial = 25000000L;
        long maximum = 250000000L;
        if (policy.hasRetryBackOff()) {
            RetryPolicy.RetryBackOff backoff = policy.getRetryBackOff();
            if (!backoff.hasBaseInterval()) {
                throw new IllegalArgumentException("xDS retry backoff requires base_interval");
            }
            long original = XdsTimeouts.nanos(backoff.getBaseInterval());
            if (original == 0) {
                throw new IllegalArgumentException("xDS retry backoff must be positive");
            }
            initial = Math.max(1000000L, original);
            if (backoff.hasMaxInterval()) {
                maximum = XdsTimeouts.nanos(backoff.getMaxInterval());
                if (maximum < original || maximum == 0) {
                    throw new IllegalArgumentException("xDS max_interval must be at least base_interval");
                }
                maximum = Math.max(1000000L, maximum);
            } else {
                maximum = initial > Long.MAX_VALUE / 10 ? Long.MAX_VALUE : initial * 10;
            }
        }
        Set<TriRpcStatus.Code> codes = EnumSet.noneOf(TriRpcStatus.Code.class);
        for (String condition : policy.getRetryOn().split(",")) {
            switch (condition.trim().toLowerCase(Locale.ROOT)) {
                case "cancelled":
                    codes.add(TriRpcStatus.Code.CANCELLED);
                    break;
                case "deadline-exceeded":
                    codes.add(TriRpcStatus.Code.DEADLINE_EXCEEDED);
                    break;
                case "internal":
                    codes.add(TriRpcStatus.Code.INTERNAL);
                    break;
                case "resource-exhausted":
                    codes.add(TriRpcStatus.Code.RESOURCE_EXHAUSTED);
                    break;
                case "unavailable":
                    codes.add(TriRpcStatus.Code.UNAVAILABLE);
                    break;
                default:
                    break;
            }
        }
        return codes.isEmpty() ? null : new XdsRetryPolicy((int) Math.min(5, retries + 1), codes, initial, maximum);
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public boolean permits(TriRpcStatus.Code code) {
        return codes.contains(code);
    }

    public long getInitialBackoff() {
        return initialBackoff;
    }

    public long nextBackoff(long current) {
        return current > maximumBackoff / 2 ? maximumBackoff : current * 2;
    }

    public long jitter(long current) {
        return ThreadLocalRandom.current().nextLong(current);
    }
}
