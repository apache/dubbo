package org.apache.dubbo.xds.resource.grpc;

final class AutoValue_GrpcAuthorizationEngine_PathMatcher extends GrpcAuthorizationEngine.PathMatcher {

  private final Matchers.StringMatcher delegate;

  AutoValue_GrpcAuthorizationEngine_PathMatcher(
      Matchers.StringMatcher delegate) {
    if (delegate == null) {
      throw new NullPointerException("Null delegate");
    }
    this.delegate = delegate;
  }

  @Override
  public Matchers.StringMatcher delegate() {
    return delegate;
  }

  @Override
  public String toString() {
    return "PathMatcher{"
        + "delegate=" + delegate
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof GrpcAuthorizationEngine.PathMatcher) {
      GrpcAuthorizationEngine.PathMatcher that = (GrpcAuthorizationEngine.PathMatcher) o;
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
