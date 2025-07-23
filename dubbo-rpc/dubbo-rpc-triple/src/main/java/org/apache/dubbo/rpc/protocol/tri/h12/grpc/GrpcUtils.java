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
package org.apache.dubbo.rpc.protocol.tri.h12.grpc;

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.http12.message.MediaType;

import java.util.concurrent.TimeUnit;

public class GrpcUtils {

    private GrpcUtils() {}

    public static Long parseTimeoutToMills(String timeoutVal) {
        if (StringUtils.isEmpty(timeoutVal) || StringUtils.isContains(timeoutVal, "null")) {
            return null;
        }
        long value = Long.parseLong(timeoutVal.substring(0, timeoutVal.length() - 1));
        char unit = timeoutVal.charAt(timeoutVal.length() - 1);
        switch (unit) {
            case 'n':
                return TimeUnit.NANOSECONDS.toMillis(value);
            case 'u':
                return TimeUnit.MICROSECONDS.toMillis(value);
            case 'm':
                return value;
            case 'S':
                return TimeUnit.SECONDS.toMillis(value);
            case 'M':
                return TimeUnit.MINUTES.toMillis(value);
            case 'H':
                return TimeUnit.HOURS.toMillis(value);
            default:
                // invalid timeout config
                return null;
        }
    }

    /**
     * Converts a timeout value to the gRPC `grpc-timeout` ASCII string format.
     * <p>
     * This method applies a greedy strategy: it chooses the largest possible unit
     * (nanos, micros, millis, seconds, minutes, hours) such that the numeric part is
     * less than 100_000_000, as required by the gRPC specification.
     * For example, a 1-second timeout will be encoded as "1000000u" (in microseconds).
     * </p>
     *
     * @param timeout the timeout value
     * @param unit the time unit of the timeout
     * @return a string suitable for use as the value of the gRPC `grpc-timeout` header
     * @throws IllegalArgumentException if the timeout too small
     */
    public static String getTimeoutHeaderValue(Long timeout, TimeUnit unit) {
        long timeoutNanos = timeout;
        if (unit != TimeUnit.NANOSECONDS) {
            timeoutNanos = unit.toNanos(timeout);
        }
        final long cutoff = 100_000_000L;
        if (timeoutNanos < 0) {
            throw new IllegalArgumentException("Timeout too small");
        } else if (timeoutNanos < cutoff) {
            return timeoutNanos + "n";
        } else if (timeoutNanos < cutoff * 1_000L) {
            return TimeUnit.NANOSECONDS.toMicros(timeoutNanos) + "u";
        } else if (timeoutNanos < cutoff * 1_000_000L) {
            return TimeUnit.NANOSECONDS.toMillis(timeoutNanos) + "m";
        } else if (timeoutNanos < cutoff * 1_000_000_000L) {
            return TimeUnit.NANOSECONDS.toSeconds(timeoutNanos) + "S";
        } else if (timeoutNanos < cutoff * 1_000_000_000L * 60L) {
            return TimeUnit.NANOSECONDS.toMinutes(timeoutNanos) + "M";
        } else {
            return TimeUnit.NANOSECONDS.toHours(timeoutNanos) + "H";
        }
    }

    public static boolean isGrpcRequest(String contentType) {
        return contentType != null && contentType.startsWith(MediaType.APPLICATION_GRPC.getName());
    }
}
