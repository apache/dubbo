package org.apache.dubbo.xds.resource.grpc.resource.filter.rbac;

public interface Matcher {
    boolean matches(Object args);
}
