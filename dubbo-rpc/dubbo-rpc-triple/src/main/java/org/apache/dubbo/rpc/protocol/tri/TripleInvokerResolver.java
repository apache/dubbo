package org.apache.dubbo.rpc.protocol.tri;


import org.apache.dubbo.rpc.Invoker;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TripleInvokerResolver implements InvokerResolver {
    private final ConcurrentHashMap<String, Invoker<?>> path2Invoker = new ConcurrentHashMap<>();
    private final Set<String> ignoreSet = ConcurrentHashMap.newKeySet();

    @Override
    public void add(String path, String service,Invoker<?> invoker) {
        // todo path == service
        Invoker<?> old = path2Invoker.putIfAbsent(service, invoker);
        if (old != null || ignoreSet.contains(service)) {
            path2Invoker.remove(service);
            ignoreSet.add(service);
        }

        path2Invoker.put(path, invoker);
    }

    @Override
    public Invoker<?> resolve(String path) {
        return path2Invoker.get(path);
    }
}
