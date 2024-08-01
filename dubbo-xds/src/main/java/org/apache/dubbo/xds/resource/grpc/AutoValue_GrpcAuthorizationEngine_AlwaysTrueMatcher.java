package org.apache.dubbo.xds.resource.grpc;

final class AutoValue_GrpcAuthorizationEngine_AlwaysTrueMatcher extends GrpcAuthorizationEngine.AlwaysTrueMatcher {

  AutoValue_GrpcAuthorizationEngine_AlwaysTrueMatcher() {
  }

  @Override
  public String toString() {
    return "AlwaysTrueMatcher{"
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof GrpcAuthorizationEngine.AlwaysTrueMatcher) {
      return true;
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    return h$;
  }

}
