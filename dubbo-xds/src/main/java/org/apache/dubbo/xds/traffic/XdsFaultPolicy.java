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
import org.apache.dubbo.xds.security.SecurityFields;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntUnaryOperator;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.Status;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.common.fault.v3.FaultDelay;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.http.fault.v3.FaultAbort;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.http.fault.v3.HTTPFault;
import io.grpc.xds.shaded.io.envoyproxy.envoy.type.v3.FractionalPercent;

/** Immutable A33 policy. Decisions own a process-wide active-fault permit, independent of RPC admission. */
public final class XdsFaultPolicy {
    private static final AtomicLong ACTIVE = new AtomicLong();
    private static final Decision NONE = new Decision(false, 0, null);
    private final boolean delayEnabled;
    private final boolean headerDelay;
    private final long delayNanos;
    private final Fraction delayFraction;
    private final boolean abortEnabled;
    private final boolean headerAbort;
    private final TriRpcStatus abortStatus;
    private final Fraction abortFraction;
    private final long maximum;

    private XdsFaultPolicy(HTTPFault config) {
        SecurityFields.only(config, "delay", "abort", "max_active_faults");
        delayEnabled = config.hasDelay();
        FaultDelay delay = config.getDelay();
        SecurityFields.only(delay, "fixed_delay", "header_delay", "percentage");
        headerDelay = delay.hasHeaderDelay();
        if (delayEnabled && !headerDelay && !delay.hasFixedDelay()) {
            throw new IllegalArgumentException("Fault delay requires fixed_delay or header_delay");
        }
        delayNanos = XdsTimeouts.nanos(delay.getFixedDelay());
        delayFraction = new Fraction(delay.getPercentage());
        abortEnabled = config.hasAbort();
        FaultAbort abort = config.getAbort();
        SecurityFields.only(abort, "http_status", "grpc_status", "header_abort", "percentage");
        headerAbort = abort.hasHeaderAbort();
        switch (abort.getErrorTypeCase()) {
            case HTTP_STATUS:
                abortStatus = httpStatus(abort.getHttpStatus());
                break;
            case GRPC_STATUS:
                abortStatus = grpcStatus(abort.getGrpcStatus());
                break;
            case HEADER_ABORT:
                abortStatus = null;
                break;
            default:
                if (abortEnabled) throw new IllegalArgumentException("Fault abort requires an error type");
                abortStatus = null;
        }
        abortFraction = new Fraction(abort.getPercentage());
        maximum = config.hasMaxActiveFaults()
                ? Integer.toUnsignedLong(config.getMaxActiveFaults().getValue())
                : Long.MAX_VALUE;
    }

    public static XdsFaultPolicy parse(Any config) {
        try {
            return parse(config.unpack(HTTPFault.class));
        } catch (InvalidProtocolBufferException error) {
            throw new IllegalArgumentException("Invalid HTTP fault configuration", error);
        }
    }

    public static XdsFaultPolicy parse(HTTPFault config) {
        return new XdsFaultPolicy(config);
    }

    public Decision decide(Map<String, String> headers) {
        return decide(headers, bound -> ThreadLocalRandom.current().nextInt(bound));
    }

    Decision decide(Map<String, String> headers, IntUnaryOperator random) {
        if (ACTIVE.get() >= maximum) return NONE;
        Long delay = null;
        TriRpcStatus abort = null;
        if (delayEnabled) {
            try {
                long nanos = delayNanos;
                String percentage = null;
                if (headerDelay) {
                    int millis = Integer.parseInt(headers.get("x-envoy-fault-delay-request"));
                    if (millis < 0) throw new NumberFormatException("Negative header delay");
                    nanos = millis * 1_000_000L;
                    percentage = headers.get("x-envoy-fault-delay-request-percentage");
                }
                if (delayFraction.selected(percentage, random)) delay = nanos;
            } catch (NumberFormatException ignored) {
                // Invalid metadata disables this header-controlled feature, independently of abort.
            }
        }
        if (abortEnabled) {
            try {
                TriRpcStatus status = abortStatus;
                String percentage = null;
                if (headerAbort) {
                    String grpc = headers.get("x-envoy-fault-abort-grpc-request");
                    String http = headers.get("x-envoy-fault-abort-request");
                    if (grpc != null) status = grpcStatus(Integer.parseInt(grpc));
                    if (http != null) status = httpStatus(Integer.parseInt(http));
                    percentage = headers.get("x-envoy-fault-abort-request-percentage");
                }
                if (status != null && abortFraction.selected(percentage, random)) {
                    abort = status.withDescription("RPC terminated due to xDS fault injection");
                }
            } catch (NumberFormatException ignored) {
                // Invalid metadata disables this header-controlled feature, independently of delay.
            }
        }
        if (delay == null && abort == null) return NONE;
        for (; ; ) {
            long active = ACTIVE.get();
            if (active >= maximum) return NONE;
            if (ACTIVE.compareAndSet(active, active + 1)) {
                return new Decision(true, delay == null ? 0 : delay, abort);
            }
        }
    }

    public static long activeFaults() {
        return ACTIVE.get();
    }

    public boolean hasActions() {
        return delayEnabled || abortEnabled;
    }

    private static TriRpcStatus grpcStatus(int code) {
        return TriRpcStatus.fromCode(Status.fromCodeValue(code).getCode().value());
    }

    private static TriRpcStatus httpStatus(int code) {
        // Use gRPC's HTTP-to-status mapping, including HTTP 200 -> UNKNOWN for missing trailers.
        switch (code) {
            case 400:
                return TriRpcStatus.INTERNAL;
            case 401:
                return TriRpcStatus.fromCode(TriRpcStatus.Code.UNAUTHENTICATED);
            case 403:
                return TriRpcStatus.fromCode(TriRpcStatus.Code.PERMISSION_DENIED);
            case 404:
                return TriRpcStatus.UNIMPLEMENTED;
            case 429:
            case 502:
            case 503:
            case 504:
                return TriRpcStatus.UNAVAILABLE;
            default:
                return TriRpcStatus.UNKNOWN;
        }
    }

    private static final class Fraction {
        private final long numerator;
        private final int denominator;

        Fraction(FractionalPercent config) {
            SecurityFields.only(config, "numerator", "denominator");
            switch (config.getDenominator()) {
                case HUNDRED:
                    denominator = 100;
                    break;
                case TEN_THOUSAND:
                    denominator = 10_000;
                    break;
                case MILLION:
                    denominator = 1_000_000;
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported fault percentage denominator");
            }
            numerator = Math.min(Integer.toUnsignedLong(config.getNumerator()), denominator);
        }

        boolean selected(String header, IntUnaryOperator random) {
            long effective = numerator;
            if (header != null) {
                int override = Integer.parseInt(header);
                if (override >= 0) effective = Math.min(effective, override);
            }
            return random.applyAsInt(1_000_000) < effective * (1_000_000 / denominator);
        }
    }

    /** Release on delay completion, local abort delivery, cancellation or failure to enqueue execution. */
    public static final class Decision implements AutoCloseable {
        private final boolean injected;
        private final long delayNanos;
        private final TriRpcStatus abort;
        private final AtomicBoolean released = new AtomicBoolean();

        private Decision(boolean injected, long delayNanos, TriRpcStatus abort) {
            this.injected = injected;
            this.delayNanos = delayNanos;
            this.abort = abort;
        }

        public boolean isInjected() {
            return injected;
        }

        public long getDelayNanos() {
            return delayNanos;
        }

        public TriRpcStatus getAbort() {
            return abort;
        }

        @Override
        public void close() {
            if (injected && released.compareAndSet(false, true)) ACTIVE.decrementAndGet();
        }
    }
}
