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
package org.apache.dubbo.xds.resource.route;

import org.apache.dubbo.common.lang.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.protobuf.Duration;
import io.grpc.Status;
import io.grpc.Status.Code;

public class RetryPolicy {

    private final int maxAttempts;

    private final List<Code> retryableStatusCodes;

    private final Duration initialBackoff;

    private final Duration maxBackoff;

    @Nullable
    private final Duration perAttemptRecvTimeout;

    public RetryPolicy(
            int maxAttempts,
            List<Code> retryableStatusCodes,
            Duration initialBackoff,
            Duration maxBackoff,
            @Nullable Duration perAttemptRecvTimeout) {
        this.maxAttempts = maxAttempts;
        if (retryableStatusCodes == null) {
            throw new NullPointerException("Null retryableStatusCodes");
        }
        this.retryableStatusCodes = Collections.unmodifiableList(new ArrayList<>(retryableStatusCodes));
        if (initialBackoff == null) {
            throw new NullPointerException("Null initialBackoff");
        }
        this.initialBackoff = initialBackoff;
        if (maxBackoff == null) {
            throw new NullPointerException("Null maxBackoff");
        }
        this.maxBackoff = maxBackoff;
        this.perAttemptRecvTimeout = perAttemptRecvTimeout;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public List<Status.Code> getRetryableStatusCodes() {
        return retryableStatusCodes;
    }

    public Duration getInitialBackoff() {
        return initialBackoff;
    }

    public Duration getMaxBackoff() {
        return maxBackoff;
    }

    @Nullable
    public Duration getPerAttemptRecvTimeout() {
        return perAttemptRecvTimeout;
    }

    public String toString() {
        return "RetryPolicy{" + "maxAttempts=" + maxAttempts + ", " + "retryableStatusCodes=" + retryableStatusCodes
                + ", " + "initialBackoff=" + initialBackoff + ", " + "maxBackoff=" + maxBackoff + ", "
                + "perAttemptRecvTimeout=" + perAttemptRecvTimeout + "}";
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof RetryPolicy) {
            RetryPolicy that = (RetryPolicy) o;
            return this.maxAttempts == that.getMaxAttempts()
                    && this.retryableStatusCodes.equals(that.getRetryableStatusCodes())
                    && this.initialBackoff.equals(that.getInitialBackoff())
                    && this.maxBackoff.equals(that.getMaxBackoff())
                    && (this.perAttemptRecvTimeout == null
                            ? that.getPerAttemptRecvTimeout() == null
                            : this.perAttemptRecvTimeout.equals(that.getPerAttemptRecvTimeout()));
        }
        return false;
    }

    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= maxAttempts;
        h$ *= 1000003;
        h$ ^= retryableStatusCodes.hashCode();
        h$ *= 1000003;
        h$ ^= initialBackoff.hashCode();
        h$ *= 1000003;
        h$ ^= maxBackoff.hashCode();
        h$ *= 1000003;
        h$ ^= (perAttemptRecvTimeout == null) ? 0 : perAttemptRecvTimeout.hashCode();
        return h$;
    }
}
