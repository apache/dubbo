package org.apache.dubbo.xds.resource.grpc;

final class AutoValue_Stats_DroppedRequests extends Stats.DroppedRequests {

  private final String category;

  private final long droppedCount;

  AutoValue_Stats_DroppedRequests(
      String category,
      long droppedCount) {
    if (category == null) {
      throw new NullPointerException("Null category");
    }
    this.category = category;
    this.droppedCount = droppedCount;
  }

  @Override
  String category() {
    return category;
  }

  @Override
  long droppedCount() {
    return droppedCount;
  }

  @Override
  public String toString() {
    return "DroppedRequests{"
        + "category=" + category + ", "
        + "droppedCount=" + droppedCount
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof Stats.DroppedRequests) {
      Stats.DroppedRequests that = (Stats.DroppedRequests) o;
      return this.category.equals(that.category())
          && this.droppedCount == that.droppedCount();
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= category.hashCode();
    h$ *= 1000003;
    h$ ^= (int) ((droppedCount >>> 32) ^ droppedCount);
    return h$;
  }

}
