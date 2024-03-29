package org.apache.dubbo.xds.auth;

public interface DemoService2 {
    default String sayHello(String name) {
        return null;
    }

}
