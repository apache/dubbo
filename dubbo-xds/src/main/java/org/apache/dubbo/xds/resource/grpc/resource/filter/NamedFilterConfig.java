package org.apache.dubbo.xds.resource.grpc.resource.filter;

import com.google.common.base.MoreObjects;

import java.util.Objects;

public class NamedFilterConfig {
    // filter instance name
    final String name;
    final FilterConfig filterConfig;

    public NamedFilterConfig(String name, FilterConfig filterConfig) {
      this.name = name;
      this.filterConfig = filterConfig;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      NamedFilterConfig that = (NamedFilterConfig) o;
      return Objects.equals(name, that.name)
          && Objects.equals(filterConfig, that.filterConfig);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, filterConfig);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("name", name)
          .add("filterConfig", filterConfig)
          .toString();
    }
  }
