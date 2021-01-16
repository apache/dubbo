package org.apache.dubbo.rpc.protocol.tri;


import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.Invoker;

import java.util.concurrent.ConcurrentHashMap;

public class TriplePathResolver implements PathResolver {
    private static final Logger logger = LoggerFactory.getLogger(TriplePathResolver.class);

    private final ConcurrentHashMap<String, Invoker<?>> path2Invoker = new ConcurrentHashMap<>();

    @Override
    public void add(String path, Invoker<?> invoker) {
        path2Invoker.put(path, invoker);
    }

    @Override
    public Invoker<?> resolve(String path) {
        return path2Invoker.get(path);
    }

    @Override
    public void remove(String path) {
        path2Invoker.remove(path);
    }

    @Override
    public void destroy() {
        path2Invoker.clear();
    }

}
