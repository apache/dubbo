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

import org.apache.dubbo.xds.security.SecurityFields;

import com.google.protobuf.Duration;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteAction;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager;

/** gRFC A31 full-stream limits. Envoy's response timer and header offset do not control a client RPC. */
public final class XdsTimeouts {
    private XdsTimeouts() {}

    public static long nanos(Duration duration) {
        SecurityFields.only(duration, "seconds", "nanos");
        long seconds = duration.getSeconds();
        int nanos = duration.getNanos();
        if (seconds < 0 || seconds > 315576000000L || nanos < 0 || nanos > 999999999) {
            throw new IllegalArgumentException("Invalid non-negative xDS duration");
        }
        if (seconds > (Long.MAX_VALUE - nanos) / 1000000000L) {
            return Long.MAX_VALUE;
        }
        return seconds * 1000000000L + nanos;
    }

    public static long listener(HttpConnectionManager hcm) {
        return hcm.getCommonHttpProtocolOptions().hasMaxStreamDuration()
                ? nanos(hcm.getCommonHttpProtocolOptions().getMaxStreamDuration())
                : 0;
    }

    public static Long route(RouteAction action) {
        if (action.hasTimeout()) {
            nanos(action.getTimeout()); // Validated but intentionally ignored, as in gRPC xDS.
        }
        if (!action.hasMaxStreamDuration()) {
            return null;
        }
        RouteAction.MaxStreamDuration limits = action.getMaxStreamDuration();
        SecurityFields.only(limits, "max_stream_duration", "grpc_timeout_header_max", "grpc_timeout_header_offset");
        if (limits.hasGrpcTimeoutHeaderOffset()) {
            nanos(limits.getGrpcTimeoutHeaderOffset()); // A proxy-only setting, not a client deadline subtraction.
        }
        if (limits.hasMaxStreamDuration()) {
            nanos(limits.getMaxStreamDuration());
        }
        if (limits.hasGrpcTimeoutHeaderMax()) {
            return nanos(limits.getGrpcTimeoutHeaderMax());
        }
        return limits.hasMaxStreamDuration() ? nanos(limits.getMaxStreamDuration()) : null;
    }
}
