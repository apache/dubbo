package org.apache.dubbo.xds.resource.grpc;

import org.apache.dubbo.common.lang.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Duration;
import io.grpc.Status;
import io.grpc.Status.Code;

final class AutoValue_VirtualHost_Route_RouteAction_RetryPolicy extends VirtualHost.Route.RouteAction.RetryPolicy {

  private final int maxAttempts;

  private final ImmutableList<Code> retryableStatusCodes;

  private final Duration initialBackoff;

  private final Duration maxBackoff;

  @Nullable
  private final Duration perAttemptRecvTimeout;

  AutoValue_VirtualHost_Route_RouteAction_RetryPolicy(
      int maxAttempts,
      ImmutableList<Status.Code> retryableStatusCodes,
      Duration initialBackoff,
      Duration maxBackoff,
      @Nullable Duration perAttemptRecvTimeout) {
    this.maxAttempts = maxAttempts;
    if (retryableStatusCodes == null) {
      throw new NullPointerException("Null retryableStatusCodes");
    }
    this.retryableStatusCodes = retryableStatusCodes;
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

  @Override
  int maxAttempts() {
    return maxAttempts;
  }

  @Override
  ImmutableList<Status.Code> retryableStatusCodes() {
    return retryableStatusCodes;
  }

  @Override
  Duration initialBackoff() {
    return initialBackoff;
  }

  @Override
  Duration maxBackoff() {
    return maxBackoff;
  }

  @Nullable
  @Override
  Duration perAttemptRecvTimeout() {
    return perAttemptRecvTimeout;
  }

  @Override
  public String toString() {
    return "RetryPolicy{"
        + "maxAttempts=" + maxAttempts + ", "
        + "retryableStatusCodes=" + retryableStatusCodes + ", "
        + "initialBackoff=" + initialBackoff + ", "
        + "maxBackoff=" + maxBackoff + ", "
        + "perAttemptRecvTimeout=" + perAttemptRecvTimeout
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof VirtualHost.Route.RouteAction.RetryPolicy) {
      VirtualHost.Route.RouteAction.RetryPolicy that = (VirtualHost.Route.RouteAction.RetryPolicy) o;
      return this.maxAttempts == that.maxAttempts()
          && this.retryableStatusCodes.equals(that.retryableStatusCodes())
          && this.initialBackoff.equals(that.initialBackoff())
          && this.maxBackoff.equals(that.maxBackoff())
          && (this.perAttemptRecvTimeout == null ? that.perAttemptRecvTimeout() == null : this.perAttemptRecvTimeout.equals(that.perAttemptRecvTimeout()));
    }
    return false;
  }

  @Override
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
