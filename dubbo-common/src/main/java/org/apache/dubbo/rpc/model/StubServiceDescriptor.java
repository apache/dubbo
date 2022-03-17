package org.apache.dubbo.rpc.model;

public class StubServiceDescriptor extends ServiceDescriptor {
    public StubServiceDescriptor(String serviceName, Class<?> interfaceClass) {
        super(serviceName, interfaceClass);
    }
}
