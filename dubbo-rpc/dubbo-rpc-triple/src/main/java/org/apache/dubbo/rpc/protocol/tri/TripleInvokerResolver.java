package org.apache.dubbo.rpc.protocol.tri;


import org.apache.dubbo.rpc.Invoker;

import java.util.concurrent.ConcurrentHashMap;

public class TripleInvokerResolver implements InvokerResolver {
    private final ConcurrentHashMap<String, Invoker<?>> path2Invoker = new ConcurrentHashMap<>();

    @Override
    public void add(String path, Invoker<?> invoker) {
        path2Invoker.put(path, invoker);
    }

    @Override
    public Invoker<?> resolve(String path) {
        return path2Invoker.get(path);
    }
}
