package org.apache.dubbo.xds.resource.grpc;

import org.apache.dubbo.xds.resource.grpc.Filter.FilterConfig;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

final class AutoValue_VirtualHost extends VirtualHost {

  private final String name;

  private final ImmutableList<String> domains;

  private final ImmutableList<VirtualHost.Route> routes;

  private final ImmutableMap<String, FilterConfig> filterConfigOverrides;

  AutoValue_VirtualHost(
      String name,
      ImmutableList<String> domains,
      ImmutableList<VirtualHost.Route> routes,
      ImmutableMap<String, Filter.FilterConfig> filterConfigOverrides) {
    if (name == null) {
      throw new NullPointerException("Null name");
    }
    this.name = name;
    if (domains == null) {
      throw new NullPointerException("Null domains");
    }
    this.domains = domains;
    if (routes == null) {
      throw new NullPointerException("Null routes");
    }
    this.routes = routes;
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
  ImmutableList<String> domains() {
    return domains;
  }

  @Override
  ImmutableList<VirtualHost.Route> routes() {
    return routes;
  }

  @Override
  ImmutableMap<String, Filter.FilterConfig> filterConfigOverrides() {
    return filterConfigOverrides;
  }

  @Override
  public String toString() {
    return "VirtualHost{"
        + "name=" + name + ", "
        + "domains=" + domains + ", "
        + "routes=" + routes + ", "
        + "filterConfigOverrides=" + filterConfigOverrides
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof VirtualHost) {
      VirtualHost that = (VirtualHost) o;
      return this.name.equals(that.name())
          && this.domains.equals(that.domains())
          && this.routes.equals(that.routes())
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
    h$ ^= domains.hashCode();
    h$ *= 1000003;
    h$ ^= routes.hashCode();
    h$ *= 1000003;
    h$ ^= filterConfigOverrides.hashCode();
    return h$;
  }

}
