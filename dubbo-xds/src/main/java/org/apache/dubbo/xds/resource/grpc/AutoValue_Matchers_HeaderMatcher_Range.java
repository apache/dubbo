package org.apache.dubbo.xds.resource.grpc;

final class AutoValue_Matchers_HeaderMatcher_Range extends Matchers.HeaderMatcher.Range {

  private final long start;

  private final long end;

  AutoValue_Matchers_HeaderMatcher_Range(
      long start,
      long end) {
    this.start = start;
    this.end = end;
  }

  @Override
  public long start() {
    return start;
  }

  @Override
  public long end() {
    return end;
  }

  @Override
  public String toString() {
    return "Range{"
        + "start=" + start + ", "
        + "end=" + end
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof Matchers.HeaderMatcher.Range) {
      Matchers.HeaderMatcher.Range that = (Matchers.HeaderMatcher.Range) o;
      return this.start == that.start()
          && this.end == that.end();
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= (int) ((start >>> 32) ^ start);
    h$ *= 1000003;
    h$ ^= (int) ((end >>> 32) ^ end);
    return h$;
  }

}
