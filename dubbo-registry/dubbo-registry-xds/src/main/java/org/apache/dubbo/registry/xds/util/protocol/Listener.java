package org.apache.dubbo.registry.xds.util.protocol;

import org.apache.dubbo.common.utils.ConcurrentHashSet;

import java.util.Objects;
import java.util.Set;

public class Listener {
    private Set<String> routeConfigNames;

    public Listener() {
        this.routeConfigNames = new ConcurrentHashSet<>();
    }

    public Listener(Set<String> routeConfigNames) {
        this.routeConfigNames = routeConfigNames;
    }

    public Set<String> getRouteConfigNames() {
        return routeConfigNames;
    }

    public void setRouteConfigNames(Set<String> routeConfigNames) {
        this.routeConfigNames = routeConfigNames;
    }

    public void mergeRouteConfigNames(Set<String> names) {
        this.routeConfigNames.addAll(names);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Listener listener = (Listener) o;
        return Objects.equals(routeConfigNames, listener.routeConfigNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(routeConfigNames);
    }

    @Override
    public String toString() {
        return "Listener{" +
                "routeConfigNames=" + routeConfigNames +
                '}';
    }
}
