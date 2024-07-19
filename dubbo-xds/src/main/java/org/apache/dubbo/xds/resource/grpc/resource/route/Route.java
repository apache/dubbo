package org.apache.dubbo.xds.resource.grpc.resource.route;

import org.apache.dubbo.common.lang.Nullable;
import org.apache.dubbo.xds.resource.grpc.resource.filter.FilterConfig;

import com.google.common.collect.ImmutableMap;

public class Route{

  private final RouteMatch routeMatch;

  @Nullable
  private final RouteAction routeAction;

  private final ImmutableMap<String, FilterConfig> filterConfigOverrides;

    Route(
      RouteMatch routeMatch,
      @Nullable RouteAction routeAction,
      ImmutableMap<String, FilterConfig> filterConfigOverrides) {
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


  RouteMatch routeMatch() {
    return routeMatch;
  }

  @Nullable

  RouteAction routeAction() {
    return routeAction;
  }


  ImmutableMap<String, FilterConfig> filterConfigOverrides() {
    return filterConfigOverrides;
  }


  public String toString() {
    return "Route{"
        + "routeMatch=" + routeMatch + ", "
        + "routeAction=" + routeAction + ", "
        + "filterConfigOverrides=" + filterConfigOverrides
        + "}";
  }


  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof Route) {
      Route that = (Route) o;
      return this.routeMatch.equals(that.routeMatch())
          && (this.routeAction == null ? that.routeAction() == null : this.routeAction.equals(that.routeAction()))
          && this.filterConfigOverrides.equals(that.filterConfigOverrides());
    }
    return false;
  }


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
