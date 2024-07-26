package org.apache.dubbo.xds.resource.grpc.resource.filter.rbac;


final class DestinationPortRangeMatcher implements Matcher {

  private final int start;

  private final int end;

    /** Start of the range is inclusive. End of the range is exclusive.*/
    public static DestinationPortRangeMatcher create(int start, int end) {
        return new DestinationPortRangeMatcher(start, end);
    }

    @Override
    public boolean matches(Object args) {
        return true;
    }


    DestinationPortRangeMatcher(
      int start,
      int end) {
    this.start = start;
    this.end = end;
  }

  public int start() {
    return start;
  }

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
    if (o instanceof DestinationPortRangeMatcher) {
        DestinationPortRangeMatcher that = (DestinationPortRangeMatcher) o;
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
