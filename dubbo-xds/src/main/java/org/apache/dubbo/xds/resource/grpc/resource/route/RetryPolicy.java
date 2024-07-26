package org.apache.dubbo.xds.resource.grpc.resource.route;

import org.apache.dubbo.common.lang.Nullable;

import com.google.protobuf.Duration;
import io.grpc.Status;
import io.grpc.Status.Code;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    int maxAttempts() {
        return maxAttempts;
    }

    List<Status.Code> retryableStatusCodes() {
        return retryableStatusCodes;
    }

    Duration initialBackoff() {
        return initialBackoff;
    }

    Duration maxBackoff() {
        return maxBackoff;
    }

    @Nullable
    Duration perAttemptRecvTimeout() {
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
            return this.maxAttempts == that.maxAttempts()
                    && this.retryableStatusCodes.equals(that.retryableStatusCodes())
                    && this.initialBackoff.equals(that.initialBackoff()) && this.maxBackoff.equals(that.maxBackoff())
                    && (
                    this.perAttemptRecvTimeout == null ? that.perAttemptRecvTimeout()
                            == null : this.perAttemptRecvTimeout.equals(that.perAttemptRecvTimeout()));
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
