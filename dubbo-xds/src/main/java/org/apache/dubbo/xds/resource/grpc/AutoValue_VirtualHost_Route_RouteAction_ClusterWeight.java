package org.apache.dubbo.xds.resource.grpc;

import org.apache.dubbo.xds.resource.grpc.Filter.FilterConfig;

import com.google.common.collect.ImmutableMap;

final class AutoValue_VirtualHost_Route_RouteAction_ClusterWeight extends VirtualHost.Route.RouteAction.ClusterWeight {

  private final String name;

  private final int weight;

  private final ImmutableMap<String, FilterConfig> filterConfigOverrides;

  AutoValue_VirtualHost_Route_RouteAction_ClusterWeight(
      String name,
      int weight,
      ImmutableMap<String, Filter.FilterConfig> filterConfigOverrides) {
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

  @Override
  String name() {
    return name;
  }

  @Override
  int weight() {
    return weight;
  }

  @Override
  ImmutableMap<String, Filter.FilterConfig> filterConfigOverrides() {
    return filterConfigOverrides;
  }

  @Override
  public String toString() {
    return "ClusterWeight{"
        + "name=" + name + ", "
        + "weight=" + weight + ", "
        + "filterConfigOverrides=" + filterConfigOverrides
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof VirtualHost.Route.RouteAction.ClusterWeight) {
      VirtualHost.Route.RouteAction.ClusterWeight that = (VirtualHost.Route.RouteAction.ClusterWeight) o;
      return this.name.equals(that.name())
          && this.weight == that.weight()
          && this.filterConfigOverrides.equals(that.filterConfigOverrides());
    }
    return false;
  }

  @Override
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
