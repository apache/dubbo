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

package org.apache.dubbo.xds.resource.grpc.resource.filter.fault;

import org.apache.dubbo.xds.resource.grpc.resource.common.ConfigOrError;
import org.apache.dubbo.xds.resource.grpc.resource.common.ThreadSafeRandom;
import org.apache.dubbo.xds.resource.grpc.resource.common.ThreadSafeRandomImpl;
import org.apache.dubbo.xds.resource.grpc.resource.filter.ClientInterceptorBuilder;
import org.apache.dubbo.xds.resource.grpc.resource.filter.Filter;

import java.util.concurrent.atomic.AtomicLong;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.Durations;
import io.envoyproxy.envoy.extensions.filters.http.fault.v3.HTTPFault;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.internal.GrpcUtil;

/**
 * HttpFault filter implementation.
 */
public final class FaultFilter implements Filter, ClientInterceptorBuilder {

    public static final FaultFilter INSTANCE = new FaultFilter(ThreadSafeRandomImpl.instance, new AtomicLong());
    @VisibleForTesting
    static final Metadata.Key<String> HEADER_DELAY_KEY = Metadata.Key.of("x-envoy-fault-delay-request",
            Metadata.ASCII_STRING_MARSHALLER);
    @VisibleForTesting
    static final Metadata.Key<String> HEADER_DELAY_PERCENTAGE_KEY = Metadata.Key.of(
            "x-envoy-fault-delay-request" + "-percentage", Metadata.ASCII_STRING_MARSHALLER);
    @VisibleForTesting
    static final Metadata.Key<String> HEADER_ABORT_HTTP_STATUS_KEY = Metadata.Key.of("x-envoy-fault-abort-request",
            Metadata.ASCII_STRING_MARSHALLER);
    @VisibleForTesting
    static final Metadata.Key<String> HEADER_ABORT_GRPC_STATUS_KEY = Metadata.Key.of(
            "x-envoy-fault-abort-grpc" + "-request", Metadata.ASCII_STRING_MARSHALLER);
    @VisibleForTesting
    static final Metadata.Key<String> HEADER_ABORT_PERCENTAGE_KEY = Metadata.Key.of(
            "x-envoy-fault-abort-request" + "-percentage", Metadata.ASCII_STRING_MARSHALLER);
    static final String TYPE_URL = "type.googleapis.com/envoy.extensions.filters.http.fault.v3.HTTPFault";

    private final ThreadSafeRandom random;
    private final AtomicLong activeFaultCounter;

    @VisibleForTesting
    FaultFilter(ThreadSafeRandom random, AtomicLong activeFaultCounter) {
        this.random = random;
        this.activeFaultCounter = activeFaultCounter;
    }

    @Override
    public String[] typeUrls() {
        return new String[] {TYPE_URL};
    }

    @Override
    public ConfigOrError<FaultConfig> parseFilterConfig(Message rawProtoMessage) {
        HTTPFault httpFaultProto;
        if (!(rawProtoMessage instanceof Any)) {
            return ConfigOrError.fromError("Invalid config type: " + rawProtoMessage.getClass());
        }
        Any anyMessage = (Any) rawProtoMessage;
        try {
            httpFaultProto = anyMessage.unpack(HTTPFault.class);
        } catch (InvalidProtocolBufferException e) {
            return ConfigOrError.fromError("Invalid proto: " + e);
        }
        return parseHttpFault(httpFaultProto);
    }

    private static ConfigOrError<FaultConfig> parseHttpFault(HTTPFault httpFault) {
        FaultDelay faultDelay = null;
        FaultAbort faultAbort = null;
        if (httpFault.hasDelay()) {
            faultDelay = parseFaultDelay(httpFault.getDelay());
        }
        if (httpFault.hasAbort()) {
            ConfigOrError<FaultAbort> faultAbortOrError = parseFaultAbort(httpFault.getAbort());
            if (faultAbortOrError.errorDetail != null) {
                return ConfigOrError.fromError(
                        "HttpFault contains invalid FaultAbort: " + faultAbortOrError.errorDetail);
            }
            faultAbort = faultAbortOrError.config;
        }
        Integer maxActiveFaults = null;
        if (httpFault.hasMaxActiveFaults()) {
            maxActiveFaults = httpFault.getMaxActiveFaults()
                    .getValue();
            if (maxActiveFaults < 0) {
                maxActiveFaults = Integer.MAX_VALUE;
            }
        }
        return ConfigOrError.fromConfig(FaultConfig.create(faultDelay, faultAbort, maxActiveFaults));
    }

    private static FaultDelay parseFaultDelay(
            io.envoyproxy.envoy.extensions.filters.common.fault.v3.FaultDelay faultDelay) {
        FractionalPercent percent = parsePercent(faultDelay.getPercentage());
        if (faultDelay.hasHeaderDelay()) {
            return FaultDelay.forHeader(percent);
        }
        return FaultDelay.forFixedDelay(Durations.toNanos(faultDelay.getFixedDelay()), percent);
    }

    @VisibleForTesting
    static ConfigOrError<FaultAbort> parseFaultAbort(
            io.envoyproxy.envoy.extensions.filters.http.fault.v3.FaultAbort faultAbort) {
        FractionalPercent percent = parsePercent(faultAbort.getPercentage());
        switch (faultAbort.getErrorTypeCase()) {
            case HEADER_ABORT:
                return ConfigOrError.fromConfig(FaultAbort.forHeader(percent));
            case HTTP_STATUS:
                return ConfigOrError.fromConfig(FaultAbort.forStatus(GrpcUtil.httpStatusToGrpcStatus(faultAbort.getHttpStatus()), percent));
            case GRPC_STATUS:
                return ConfigOrError.fromConfig(FaultAbort.forStatus(Status.fromCodeValue(faultAbort.getGrpcStatus())
                        , percent));
            case ERRORTYPE_NOT_SET:
            default:
                return ConfigOrError.fromError("Unknown error type case: " + faultAbort.getErrorTypeCase());
        }
    }

    private static FractionalPercent parsePercent(io.envoyproxy.envoy.type.v3.FractionalPercent proto) {
        switch (proto.getDenominator()) {
            case HUNDRED:
                return FractionalPercent.perHundred(proto.getNumerator());
            case TEN_THOUSAND:
                return FractionalPercent.perTenThousand(proto.getNumerator());
            case MILLION:
                return FractionalPercent.perMillion(proto.getNumerator());
            case UNRECOGNIZED:
            default:
                throw new IllegalArgumentException("Unknown denominator type: " + proto.getDenominator());
        }
    }

    @Override
    public ConfigOrError<FaultConfig> parseFilterConfigOverride(Message rawProtoMessage) {
        return parseFilterConfig(rawProtoMessage);
    }
}
