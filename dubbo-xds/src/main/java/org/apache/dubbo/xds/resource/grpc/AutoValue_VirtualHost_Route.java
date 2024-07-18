package org.apache.dubbo.xds.resource.grpc;

import org.apache.dubbo.common.lang.Nullable;
import org.apache.dubbo.xds.resource.grpc.Filter.FilterConfig;

import com.google.common.collect.ImmutableMap;

final class AutoValue_VirtualHost_Route extends VirtualHost.Route {

  private final VirtualHost.Route.RouteMatch routeMatch;

  @Nullable
  private final VirtualHost.Route.RouteAction routeAction;

  private final ImmutableMap<String, FilterConfig> filterConfigOverrides;

  AutoValue_VirtualHost_Route(
      VirtualHost.Route.RouteMatch routeMatch,
      @Nullable VirtualHost.Route.RouteAction routeAction,
      ImmutableMap<String, Filter.FilterConfig> filterConfigOverrides) {
    if (routeMatch == null) {
      throw new NullPointerException("Null routeMatch");
    }
    this.routeMatch = routeMatch;
    this.routeAction = routeAction;
    if (filterConfigOverrides == null) {
      throw new NullPointerException("Null filterConfigOverrides");
    }
    this.filterConfigOverrides = filterConfigOverrides;
  }

  @Override
  VirtualHost.Route.RouteMatch routeMatch() {
    return routeMatch;
  }

  @Nullable
  @Override
  VirtualHost.Route.RouteAction routeAction() {
    return routeAction;
  }

  @Override
  ImmutableMap<String, Filter.FilterConfig> filterConfigOverrides() {
    return filterConfigOverrides;
  }

  @Override
  public String toString() {
    return "Route{"
        + "routeMatch=" + routeMatch + ", "
        + "routeAction=" + routeAction + ", "
        + "filterConfigOverrides=" + filterConfigOverrides
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof VirtualHost.Route) {
      VirtualHost.Route that = (VirtualHost.Route) o;
      return this.routeMatch.equals(that.routeMatch())
          && (this.routeAction == null ? that.routeAction() == null : this.routeAction.equals(that.routeAction()))
          && this.filterConfigOverrides.equals(that.filterConfigOverrides());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= routeMatch.hashCode();
    h$ *= 1000003;
    h$ ^= (routeAction == null) ? 0 : routeAction.hashCode();
    h$ *= 1000003;
    h$ ^= filterConfigOverrides.hashCode();
    return h$;
  }

}
