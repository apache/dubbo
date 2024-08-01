package org.apache.dubbo.xds.resource.grpc;

import org.apache.dubbo.xds.resource.grpc.GrpcAuthorizationEngine.Matcher;

import com.google.common.collect.ImmutableList;

final class AutoValue_GrpcAuthorizationEngine_AndMatcher extends GrpcAuthorizationEngine.AndMatcher {

  private final ImmutableList<? extends Matcher> allMatch;

  AutoValue_GrpcAuthorizationEngine_AndMatcher(
      ImmutableList<? extends GrpcAuthorizationEngine.Matcher> allMatch) {
    if (allMatch == null) {
      throw new NullPointerException("Null allMatch");
    }
    this.allMatch = allMatch;
  }

  @Override
  public ImmutableList<? extends GrpcAuthorizationEngine.Matcher> allMatch() {
    return allMatch;
  }

  @Override
  public String toString() {
    return "AndMatcher{"
        + "allMatch=" + allMatch
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof GrpcAuthorizationEngine.AndMatcher) {
      GrpcAuthorizationEngine.AndMatcher that = (GrpcAuthorizationEngine.AndMatcher) o;
      return this.allMatch.equals(that.allMatch());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= allMatch.hashCode();
    return h$;
  }

}
