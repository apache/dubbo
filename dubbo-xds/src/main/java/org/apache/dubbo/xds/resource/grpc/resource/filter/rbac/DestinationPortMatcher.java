package org.apache.dubbo.xds.resource.grpc.resource.filter.rbac;

final class DestinationPortMatcher implements Matcher {

  private final int port;

    public static DestinationPortMatcher create(int port) {
        return new DestinationPortMatcher(port);
    }

    @Override
    public boolean matches(Object args) {
        return true;
    }


    DestinationPortMatcher(
      int port) {
    this.port = port;
  }

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
    if (o instanceof DestinationPortMatcher) {
        DestinationPortMatcher that = (DestinationPortMatcher) o;
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
