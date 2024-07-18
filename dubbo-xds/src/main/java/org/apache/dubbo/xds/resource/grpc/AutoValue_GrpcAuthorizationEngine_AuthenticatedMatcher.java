package org.apache.dubbo.xds.resource.grpc;

import org.apache.dubbo.common.lang.Nullable;

final class AutoValue_GrpcAuthorizationEngine_AuthenticatedMatcher extends GrpcAuthorizationEngine.AuthenticatedMatcher {

  @Nullable
  private final Matchers.StringMatcher delegate;

  AutoValue_GrpcAuthorizationEngine_AuthenticatedMatcher(
      @Nullable Matchers.StringMatcher delegate) {
    this.delegate = delegate;
  }

  @Nullable
  @Override
  public Matchers.StringMatcher delegate() {
    return delegate;
  }

  @Override
  public String toString() {
    return "AuthenticatedMatcher{"
        + "delegate=" + delegate
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof GrpcAuthorizationEngine.AuthenticatedMatcher) {
      GrpcAuthorizationEngine.AuthenticatedMatcher that = (GrpcAuthorizationEngine.AuthenticatedMatcher) o;
      return (this.delegate == null ? that.delegate() == null : this.delegate.equals(that.delegate()));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= (delegate == null) ? 0 : delegate.hashCode();
    return h$;
  }

}
