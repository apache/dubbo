package org.apache.dubbo.rpc.protocol.rest;

import org.apache.dubbo.metadata.rest.PathMatcher;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.protocol.rest.exception.PathNoFoundException;
import org.apache.dubbo.rpc.protocol.rest.util.Pair;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PathAndInvokerMapper {
    private static final Map<PathMatcher, Pair<Invoker, RestMethodMetadata>> pathToServiceMap = new ConcurrentHashMap<>();


    public static void addPathAndInvoker(Map<PathMatcher, RestMethodMetadata> metadataMap, Invoker invoker, String contextPath) {

        metadataMap.entrySet().stream().forEach(entry -> {
            PathMatcher pathMatcher = entry.getKey();
            pathMatcher.setContextPath(contextPath);
            pathToServiceMap.put(pathMatcher, Pair.make(invoker, entry.getValue()));
        });
    }


    public static Pair<Invoker, RestMethodMetadata> getRestMethodMetadata(String path, String version, String group, int port) {


        PathMatcher pathMather = PathMatcher.getInvokeCreatePathMatcher(path, version, group, port);

        if (!pathToServiceMap.containsKey(pathMather)) {
            throw new PathNoFoundException("rest service Path no found, current path info:" + pathMather);
        }

        return pathToServiceMap.get(pathMather);
    }
}
