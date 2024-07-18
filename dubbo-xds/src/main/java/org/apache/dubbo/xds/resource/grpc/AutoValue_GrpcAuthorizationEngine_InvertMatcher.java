package org.apache.dubbo.xds.resource.grpc;

final class AutoValue_GrpcAuthorizationEngine_InvertMatcher extends GrpcAuthorizationEngine.InvertMatcher {

  private final GrpcAuthorizationEngine.Matcher toInvertMatcher;

  AutoValue_GrpcAuthorizationEngine_InvertMatcher(
      GrpcAuthorizationEngine.Matcher toInvertMatcher) {
    if (toInvertMatcher == null) {
      throw new NullPointerException("Null toInvertMatcher");
    }
    this.toInvertMatcher = toInvertMatcher;
  }

  @Override
  public GrpcAuthorizationEngine.Matcher toInvertMatcher() {
    return toInvertMatcher;
  }

  @Override
  public String toString() {
    return "InvertMatcher{"
        + "toInvertMatcher=" + toInvertMatcher
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof GrpcAuthorizationEngine.InvertMatcher) {
      GrpcAuthorizationEngine.InvertMatcher that = (GrpcAuthorizationEngine.InvertMatcher) o;
      return this.toInvertMatcher.equals(that.toInvertMatcher());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= toInvertMatcher.hashCode();
    return h$;
  }

}
