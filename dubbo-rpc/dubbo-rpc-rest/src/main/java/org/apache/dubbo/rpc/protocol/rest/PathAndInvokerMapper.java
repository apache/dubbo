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
import org.apache.dubbo.rpc.protocol.rest.pair.InvokerAndRestMethodMetadataPair;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * save the path & metadata info mapping
 */
public class PathAndInvokerMapper {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(PathAndInvokerMapper.class);

    private final Map<PathMatcher, InvokerAndRestMethodMetadataPair> pathToServiceMapContainPathVariable = new ConcurrentHashMap<>();
    private final Map<PathMatcher, InvokerAndRestMethodMetadataPair> pathToServiceMapNoPathVariable = new ConcurrentHashMap<>();


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
                addPathMatcherToPathMap(pathMatcher, pathToServiceMapContainPathVariable, InvokerAndRestMethodMetadataPair.pair(invoker, entry.getValue()));
            } else {
                addPathMatcherToPathMap(pathMatcher, pathToServiceMapNoPathVariable, InvokerAndRestMethodMetadataPair.pair(invoker, entry.getValue()));
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
    public InvokerAndRestMethodMetadataPair getRestMethodMetadata(String path, String version, String group, Integer port) {


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

        InvokerAndRestMethodMetadataPair containPathVariablePair = pathToServiceMapContainPathVariable.remove(pathMatcher);

        InvokerAndRestMethodMetadataPair unContainPathVariablePair = pathToServiceMapNoPathVariable.remove(pathMatcher);
        logger.info("dubbo rest undeploy pathMatcher:" + pathMatcher
            + ", and path variable method is :" + (containPathVariablePair == null ? null : containPathVariablePair.getRestMethodMetadata().getReflectMethod())
            + ", and no path variable  method is :" + (unContainPathVariablePair == null ? null : unContainPathVariablePair.getRestMethodMetadata().getReflectMethod()));


    }

    public void addPathMatcherToPathMap(PathMatcher pathMatcher,
                                        Map<PathMatcher, InvokerAndRestMethodMetadataPair> pathMatcherPairMap,
                                        InvokerAndRestMethodMetadataPair invokerRestMethodMetadataPair) {

        if (pathMatcherPairMap.containsKey(pathMatcher)) {

            InvokerAndRestMethodMetadataPair beforeMetadata = pathMatcherPairMap.get(pathMatcher);

            throw new DoublePathCheckException(
                "dubbo rest double path check error, current path is: " + pathMatcher
                    + " ,and service method is: " + invokerRestMethodMetadataPair.getRestMethodMetadata().getReflectMethod()
                    + "before service  method is: " + beforeMetadata.getRestMethodMetadata().getReflectMethod()
            );
        }

        pathMatcherPairMap.put(pathMatcher, invokerRestMethodMetadataPair);


        logger.info("dubbo rest deploy pathMatcher:" + pathMatcher + ", and service method is :" + invokerRestMethodMetadataPair.getRestMethodMetadata().getReflectMethod());
    }

}
