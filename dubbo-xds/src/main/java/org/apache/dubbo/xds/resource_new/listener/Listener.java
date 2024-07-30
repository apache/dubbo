package org.apache.dubbo.xds.resource_new.listener;

import org.apache.dubbo.common.lang.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Listener {

    private String name;

    @Nullable
    private String address;

    @Nullable
    private List<FilterChain> filterChains;

    private FilterChain defaultFilterChain;

    public Listener(String name, String address, List<FilterChain> filterChains, FilterChain defaultFilterChain) {
        if (name == null) {
            throw new NullPointerException("Null name");
        }
        this.name = name;
        this.address = address;
        if (filterChains == null) {
            throw new NullPointerException("Null filterChains");
        }
        this.filterChains = Collections.unmodifiableList(new ArrayList<>(filterChains));
        this.defaultFilterChain = defaultFilterChain;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Nullable
    public String address() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Nullable
    public List<FilterChain> filterChains() {
        return filterChains;
    }

    public void setFilterChains(List<FilterChain> filterChains) {
        this.filterChains = filterChains;
    }

    public FilterChain defaultFilterChain() {
        return defaultFilterChain;
    }

    public void setDefaultFilterChain(FilterChain defaultFilterChain) {
        this.defaultFilterChain = defaultFilterChain;
    }

    public String toString() {
        return "Listener{" + "name='" + name + '\'' + ", address='" + address + '\'' + ", filterChains=" + filterChains
                + ", defaultFilterChain=" + defaultFilterChain + '}';
    }

    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}
        Listener listener = (Listener) o;
        return Objects.equals(name, listener.name) && Objects.equals(address, listener.address)
                && Objects.equals(filterChains, listener.filterChains)
                && Objects.equals(defaultFilterChain, listener.defaultFilterChain);
    }

    public int hashCode() {
        return Objects.hash(name, address, filterChains, defaultFilterChain);
    }

    public static Listener create(
            String name,
            @Nullable String address,
            List<FilterChain> filterChains,
            @Nullable FilterChain defaultFilterChain) {
        return new Listener(name, address, filterChains, defaultFilterChain);
    }
}
