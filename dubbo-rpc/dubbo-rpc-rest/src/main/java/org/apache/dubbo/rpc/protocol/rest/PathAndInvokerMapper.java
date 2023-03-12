/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.rpc.protocol.rest;

import org.apache.dubbo.metadata.rest.PathMatcher;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.protocol.rest.exception.PathNoFoundException;
import org.apache.dubbo.rpc.protocol.rest.util.Pair;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PathAndInvokerMapper {
    private static final Map<PathMatcher, Pair<Invoker, RestMethodMetadata>> pathToServiceMapContainPathVariable = new ConcurrentHashMap<>();
    private static final Map<PathMatcher, Pair<Invoker, RestMethodMetadata>> pathToServiceMapNoPathVariable = new ConcurrentHashMap<>();


    public static void addPathAndInvoker(Map<PathMatcher, RestMethodMetadata> metadataMap, Invoker invoker) {

        metadataMap.entrySet().stream().forEach(entry -> {
            PathMatcher pathMatcher = entry.getKey();
            if (pathMatcher.hasPathVariable()) {
                pathToServiceMapContainPathVariable.put(pathMatcher, Pair.make(invoker, entry.getValue()));
            } else {
                pathToServiceMapNoPathVariable.put(pathMatcher, Pair.make(invoker, entry.getValue()));
            }
        });
    }


    public static Pair<Invoker, RestMethodMetadata> getRestMethodMetadata(String path, String version, String group, int port) {


        PathMatcher pathMather = PathMatcher.getInvokeCreatePathMatcher(path, version, group, port);

        // first search from pathToServiceMapNoPathVariable
        if (pathToServiceMapNoPathVariable.containsKey(pathMather)) {
            return pathToServiceMapNoPathVariable.get(pathMather);
        }

        // second search from pathToServiceMapNoPathVariable
        if (pathToServiceMapContainPathVariable.containsKey(pathMather)) {
            return pathToServiceMapContainPathVariable.get(pathMather);
        }

        throw new PathNoFoundException("rest service Path no found, current path info:" + pathMather);
    }

    public static void removePath(PathMatcher pathMatcher) {
        pathToServiceMapContainPathVariable.remove(pathMatcher);

        pathToServiceMapNoPathVariable.remove(pathMatcher);
    }
}
