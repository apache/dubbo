package org.apache.dubbo.xds.resource_new.filter.rbac;

public interface Matcher {
    boolean matches(Object args);
}
