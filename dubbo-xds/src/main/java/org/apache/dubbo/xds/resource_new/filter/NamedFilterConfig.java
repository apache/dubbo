package org.apache.dubbo.xds.resource_new.filter;

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
        return Objects.equals(name, that.name) && Objects.equals(filterConfig, that.filterConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, filterConfig);
    }

    @Override
    public String toString() {
        return "NamedFilterConfig{" + "name='" + name + '\'' + ", filterConfig=" + filterConfig + '}';
    }
}
