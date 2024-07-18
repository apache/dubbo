package org.apache.dubbo.xds.resource.grpc;

final class AutoValue_GrpcAuthorizationEngine_DestinationIpMatcher extends GrpcAuthorizationEngine.DestinationIpMatcher {

  private final Matchers.CidrMatcher delegate;

  AutoValue_GrpcAuthorizationEngine_DestinationIpMatcher(
      Matchers.CidrMatcher delegate) {
    if (delegate == null) {
      throw new NullPointerException("Null delegate");
    }
    this.delegate = delegate;
  }

  @Override
  public Matchers.CidrMatcher delegate() {
    return delegate;
  }

  @Override
  public String toString() {
    return "DestinationIpMatcher{"
        + "delegate=" + delegate
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof GrpcAuthorizationEngine.DestinationIpMatcher) {
      GrpcAuthorizationEngine.DestinationIpMatcher that = (GrpcAuthorizationEngine.DestinationIpMatcher) o;
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
