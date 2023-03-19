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

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.metadata.rest.PathMatcher;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.protocol.rest.exception.DoublePathCheckException;
import org.apache.dubbo.rpc.protocol.rest.exception.PathNoFoundException;
import org.apache.dubbo.rpc.protocol.rest.util.Pair;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * save the path & metadata info mapping
 */
public class PathAndInvokerMapper {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(PathAndInvokerMapper.class);

    private final Map<PathMatcher, Pair<Invoker, RestMethodMetadata>> pathToServiceMapContainPathVariable = new ConcurrentHashMap<>();
    private final Map<PathMatcher, Pair<Invoker, RestMethodMetadata>> pathToServiceMapNoPathVariable = new ConcurrentHashMap<>();


    /**
     * deploy path metadata
     *
     * @param metadataMap
     * @param invoker
     */
    public void addPathAndInvoker(Map<PathMatcher, RestMethodMetadata> metadataMap, Invoker invoker) {

        metadataMap.entrySet().stream().forEach(entry -> {
            PathMatcher pathMatcher = entry.getKey();
            if (pathMatcher.hasPathVariable()) {
                addPathMatcherToPathMap(pathMatcher, pathToServiceMapContainPathVariable, Pair.make(invoker, entry.getValue()));
            } else {
                addPathMatcherToPathMap(pathMatcher, pathToServiceMapNoPathVariable, Pair.make(invoker, entry.getValue()));
            }
        });
    }

    /**
     * acquire metadata & invoker by service info
     *
     * @param path
     * @param version
     * @param group
     * @param port
     * @return
     */
    public Pair<Invoker, RestMethodMetadata> getRestMethodMetadata(String path, String version, String group, Integer port) {


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

    /**
     * undeploy path metadata
     *
     * @param pathMatcher
     */
    public void removePath(PathMatcher pathMatcher) {

        Pair<Invoker, RestMethodMetadata> containPathVariablePair = pathToServiceMapContainPathVariable.remove(pathMatcher);

        Pair<Invoker, RestMethodMetadata> unContainPathVariablePair = pathToServiceMapNoPathVariable.remove(pathMatcher);
        logger.info("dubbo rest undeploy pathMatcher:" + pathMatcher
            + ", and path variable metadata is :" + (containPathVariablePair == null ? null : containPathVariablePair.getSecond())
            + ", and no path variable  metadata is :" + (unContainPathVariablePair == null ? null : unContainPathVariablePair.getSecond()));


    }

    public void addPathMatcherToPathMap(PathMatcher pathMatcher,
                                        Map<PathMatcher, Pair<Invoker, RestMethodMetadata>> pathMatcherPairMap,
                                        Pair<Invoker, RestMethodMetadata> invokerRestMethodMetadataPair) {

        if (pathMatcherPairMap.containsKey(pathMatcher)) {

            Pair<Invoker, RestMethodMetadata> beforeMetadata = pathMatcherPairMap.get(pathMatcher);

            throw new DoublePathCheckException(
                "dubbo rest double path check error, current path is: " + pathMatcher
                    + " ,and metadata is: " + invokerRestMethodMetadataPair.getSecond()
                    + "before  metadata is: " + beforeMetadata.getSecond()
            );
        }

        pathMatcherPairMap.put(pathMatcher, invokerRestMethodMetadataPair);


        logger.info("dubbo rest deploy pathMatcher:" + pathMatcher + ", and metadata is :" + invokerRestMethodMetadataPair.getSecond());
    }

}
