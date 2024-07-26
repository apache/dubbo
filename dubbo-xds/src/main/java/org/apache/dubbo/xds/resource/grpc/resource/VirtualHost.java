package org.apache.dubbo.xds.resource.grpc.resource;

import org.apache.dubbo.xds.resource.grpc.resource.filter.FilterConfig;
import org.apache.dubbo.xds.resource.grpc.resource.route.Route;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class VirtualHost {

  private String name;
  private List<String> domains;
  private List<Route> routes;
  private Map<String, FilterConfig> filterConfigOverrides;

  public VirtualHost(
      String name,
      List<String> domains,
      List<Route> routes,
      Map<String, FilterConfig> filterConfigOverrides) {
    this.name = name;
    this.domains = Collections.unmodifiableList(new ArrayList<>(domains));
    this.routes = Collections.unmodifiableList(new ArrayList<>(routes));
    this.filterConfigOverrides = Collections.unmodifiableMap(new HashMap<>(filterConfigOverrides));
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<String> getDomains() {
    return domains;
  }

  public void setDomains(List<String> domains) {
    this.domains = new ArrayList<>(domains);
  }

  public List<Route> getRoutes() {
    return routes;
  }

  public void setRoutes(List<Route> routes) {
    this.routes = new ArrayList<>(routes);
  }

  public Map<String, FilterConfig> getFilterConfigOverrides() {
    return filterConfigOverrides;
  }

  public void setFilterConfigOverrides(Map<String, FilterConfig> filterConfigOverrides) {
    this.filterConfigOverrides = new HashMap<>(filterConfigOverrides);
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
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    VirtualHost that = (VirtualHost) o;
    return Objects.equals(name, that.name)
        && Objects.equals(domains, that.domains)
        && Objects.equals(routes, that.routes)
        && Objects.equals(filterConfigOverrides, that.filterConfigOverrides);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, domains, routes, filterConfigOverrides);
  }

    public static VirtualHost create(
            String name, List<String> domains, List<Route> routes,
            Map<String, FilterConfig> filterConfigOverrides) {
        return new VirtualHost(name, domains, routes, filterConfigOverrides);
    }
}
