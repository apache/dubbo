package org.apache.dubbo.xds.resource.grpc;

final class AutoValue_GrpcAuthorizationEngine_DestinationPortRangeMatcher extends GrpcAuthorizationEngine.DestinationPortRangeMatcher {

  private final int start;

  private final int end;

  AutoValue_GrpcAuthorizationEngine_DestinationPortRangeMatcher(
      int start,
      int end) {
    this.start = start;
    this.end = end;
  }

  @Override
  public int start() {
    return start;
  }

  @Override
  public int end() {
    return end;
  }

  @Override
  public String toString() {
    return "DestinationPortRangeMatcher{"
        + "start=" + start + ", "
        + "end=" + end
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof GrpcAuthorizationEngine.DestinationPortRangeMatcher) {
      GrpcAuthorizationEngine.DestinationPortRangeMatcher that = (GrpcAuthorizationEngine.DestinationPortRangeMatcher) o;
      return this.start == that.start()
          && this.end == that.end();
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= start;
    h$ *= 1000003;
    h$ ^= end;
    return h$;
  }

}
