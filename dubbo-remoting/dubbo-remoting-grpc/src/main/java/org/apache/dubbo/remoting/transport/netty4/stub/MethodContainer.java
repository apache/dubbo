package org.apache.dubbo.remoting.transport.netty4.stub;

public interface MethodContainer {

    ServerMethodModel lookup(String path);

    void add(String path, ServerMethodModel method);
}
