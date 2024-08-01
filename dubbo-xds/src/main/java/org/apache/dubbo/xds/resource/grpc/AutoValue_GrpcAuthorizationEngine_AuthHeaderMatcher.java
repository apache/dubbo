package org.apache.dubbo.xds.resource.grpc;

final class AutoValue_GrpcAuthorizationEngine_AuthHeaderMatcher extends GrpcAuthorizationEngine.AuthHeaderMatcher {

  private final Matchers.HeaderMatcher delegate;

  AutoValue_GrpcAuthorizationEngine_AuthHeaderMatcher(
      Matchers.HeaderMatcher delegate) {
    if (delegate == null) {
      throw new NullPointerException("Null delegate");
    }
    this.delegate = delegate;
  }

  @Override
  public Matchers.HeaderMatcher delegate() {
    return delegate;
  }

  @Override
  public String toString() {
    return "AuthHeaderMatcher{"
        + "delegate=" + delegate
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof GrpcAuthorizationEngine.AuthHeaderMatcher) {
      GrpcAuthorizationEngine.AuthHeaderMatcher that = (GrpcAuthorizationEngine.AuthHeaderMatcher) o;
      return this.delegate.equals(that.delegate());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= delegate.hashCode();
    return h$;
  }

}
