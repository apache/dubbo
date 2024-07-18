package org.apache.dubbo.xds.resource.grpc;

import org.apache.dubbo.xds.resource.grpc.GrpcAuthorizationEngine.Matcher;

import com.google.common.collect.ImmutableList;

final class AutoValue_GrpcAuthorizationEngine_OrMatcher extends GrpcAuthorizationEngine.OrMatcher {

  private final ImmutableList<? extends Matcher> anyMatch;

  AutoValue_GrpcAuthorizationEngine_OrMatcher(
      ImmutableList<? extends GrpcAuthorizationEngine.Matcher> anyMatch) {
    if (anyMatch == null) {
      throw new NullPointerException("Null anyMatch");
    }
    this.anyMatch = anyMatch;
  }

  @Override
  public ImmutableList<? extends GrpcAuthorizationEngine.Matcher> anyMatch() {
    return anyMatch;
  }

  @Override
  public String toString() {
    return "OrMatcher{"
        + "anyMatch=" + anyMatch
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof GrpcAuthorizationEngine.OrMatcher) {
      GrpcAuthorizationEngine.OrMatcher that = (GrpcAuthorizationEngine.OrMatcher) o;
      return this.anyMatch.equals(that.anyMatch());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= anyMatch.hashCode();
    return h$;
  }

}
