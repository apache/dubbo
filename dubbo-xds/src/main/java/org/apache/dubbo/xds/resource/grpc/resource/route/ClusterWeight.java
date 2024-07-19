package org.apache.dubbo.xds.resource.grpc.resource.route;

import org.apache.dubbo.xds.resource.grpc.resource.filter.FilterConfig;

import com.google.common.collect.ImmutableMap;

public class ClusterWeight {

  private final String name;

  private final int weight;

  private final ImmutableMap<String, FilterConfig> filterConfigOverrides;

    ClusterWeight(
      String name,
      int weight,
      ImmutableMap<String, FilterConfig> filterConfigOverrides) {
    if (name == null) {
      throw new NullPointerException("Null name");
    }
    this.name = name;
    this.weight = weight;
    if (filterConfigOverrides == null) {
      throw new NullPointerException("Null filterConfigOverrides");
    }
    this.filterConfigOverrides = filterConfigOverrides;
  }


  String name() {
    return name;
  }


  int weight() {
    return weight;
  }


  ImmutableMap<String, FilterConfig> filterConfigOverrides() {
    return filterConfigOverrides;
  }


  public String toString() {
    return "ClusterWeight{"
        + "name=" + name + ", "
        + "weight=" + weight + ", "
        + "filterConfigOverrides=" + filterConfigOverrides
        + "}";
  }


  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof ClusterWeight) {
      ClusterWeight that = (ClusterWeight) o;
      return this.name.equals(that.name())
          && this.weight == that.weight()
          && this.filterConfigOverrides.equals(that.filterConfigOverrides());
    }
    return false;
  }


  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= name.hashCode();
    h$ *= 1000003;
    h$ ^= weight;
    h$ *= 1000003;
    h$ ^= filterConfigOverrides.hashCode();
    return h$;
  }

}
