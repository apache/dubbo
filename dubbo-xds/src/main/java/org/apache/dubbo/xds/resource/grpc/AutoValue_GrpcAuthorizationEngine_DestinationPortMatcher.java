package org.apache.dubbo.xds.resource.grpc;

final class AutoValue_GrpcAuthorizationEngine_DestinationPortMatcher extends GrpcAuthorizationEngine.DestinationPortMatcher {

  private final int port;

  AutoValue_GrpcAuthorizationEngine_DestinationPortMatcher(
      int port) {
    this.port = port;
  }

  @Override
  public int port() {
    return port;
  }

  @Override
  public String toString() {
    return "DestinationPortMatcher{"
        + "port=" + port
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof GrpcAuthorizationEngine.DestinationPortMatcher) {
      GrpcAuthorizationEngine.DestinationPortMatcher that = (GrpcAuthorizationEngine.DestinationPortMatcher) o;
      return this.port == that.port();
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= port;
    return h$;
  }

}
