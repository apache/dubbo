package org.apache.dubbo.xds.resource.grpc;

import javax.annotation.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_Endpoints_DropOverload extends Endpoints.DropOverload {

  private final String category;

  private final int dropsPerMillion;

  AutoValue_Endpoints_DropOverload(
      String category,
      int dropsPerMillion) {
    if (category == null) {
      throw new NullPointerException("Null category");
    }
    this.category = category;
    this.dropsPerMillion = dropsPerMillion;
  }

  @Override
  String category() {
    return category;
  }

  @Override
  int dropsPerMillion() {
    return dropsPerMillion;
  }

  @Override
  public String toString() {
    return "DropOverload{"
        + "category=" + category + ", "
        + "dropsPerMillion=" + dropsPerMillion
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof Endpoints.DropOverload) {
      Endpoints.DropOverload that = (Endpoints.DropOverload) o;
      return this.category.equals(that.category())
          && this.dropsPerMillion == that.dropsPerMillion();
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= category.hashCode();
    h$ *= 1000003;
    h$ ^= dropsPerMillion;
    return h$;
  }

}
