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
import org.apache.dubbo.rpc.protocol.rest.pair.InvokerAndRestMethodMetadataPair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * save the path & metadata info mapping
 */
public class PathAndInvokerMapper {
    private static final ErrorTypeAwareLogger logger =
            LoggerFactory.getErrorTypeAwareLogger(PathAndInvokerMapper.class);

    private final Map<PathMatcher, InvokerAndRestMethodMetadataPair> pathToServiceMapContainPathVariable =
            new ConcurrentHashMap<>();
    private final Map<PathMatcher, InvokerAndRestMethodMetadataPair> pathToServiceMapNoPathVariable =
            new ConcurrentHashMap<>();

    // for http method compare 405
    private final Map<PathMatcher, Set<String>> pathMatcherToHttpMethodMap = new HashMap<>();

    /**
     * deploy path metadata
     *
     * @param metadataMap
     * @param invoker
     */
    public void addPathAndInvoker(Map<PathMatcher, RestMethodMetadata> metadataMap, Invoker invoker) {

        metadataMap.forEach((pathMatcher, value) -> {
            if (pathMatcher.hasPathVariable()) {
                addPathMatcherToPathMap(
                        pathMatcher,
                        pathToServiceMapContainPathVariable,
                        InvokerAndRestMethodMetadataPair.pair(invoker, value));
            } else {
                addPathMatcherToPathMap(
                        pathMatcher,
                        pathToServiceMapNoPathVariable,
                        InvokerAndRestMethodMetadataPair.pair(invoker, value));
            }
        });
    }

    /**
     * get rest method metadata by path matcher
     *
     * @param pathMatcher
     * @return
     */
    public InvokerAndRestMethodMetadataPair getRestMethodMetadata(PathMatcher pathMatcher) {

        // first search from pathToServiceMapNoPathVariable
        InvokerAndRestMethodMetadataPair pair;
        pair = pathToServiceMapNoPathVariable.get(pathMatcher);
        if (pair == null) {
            // second search from pathToServiceMapContainPathVariable
            pair = pathToServiceMapContainPathVariable.get(pathMatcher);
        }

        return pair;
    }

    /**
     * undeploy path metadata
     *
     * @param pathMatcher
     */
    public void removePath(PathMatcher pathMatcher) {

        InvokerAndRestMethodMetadataPair containPathVariablePair =
                pathToServiceMapContainPathVariable.remove(pathMatcher);

        InvokerAndRestMethodMetadataPair unContainPathVariablePair = pathToServiceMapNoPathVariable.remove(pathMatcher);
        logger.info("dubbo rest undeploy pathMatcher:" + pathMatcher
                + ", and path variable method is :"
                + (containPathVariablePair == null
                        ? null
                        : containPathVariablePair.getRestMethodMetadata().getReflectMethod())
                + ", and no path variable  method is :"
                + (unContainPathVariablePair == null
                        ? null
                        : unContainPathVariablePair.getRestMethodMetadata().getReflectMethod()));
    }

    public void addPathMatcherToPathMap(
            PathMatcher pathMatcher,
            Map<PathMatcher, InvokerAndRestMethodMetadataPair> pathMatcherPairMap,
            InvokerAndRestMethodMetadataPair invokerRestMethodMetadataPair) {

        InvokerAndRestMethodMetadataPair beforeMetadata = pathMatcherPairMap.get(pathMatcher);
        if (beforeMetadata != null) {
            // cover the old service metadata when current interface is old interface & current method desc equals
            // old`s method desc,else ,throw double check exception
            // true when reExport
            if (!invokerRestMethodMetadataPair.compareServiceMethod(beforeMetadata)) {
                throw new DoublePathCheckException("dubbo rest double path check error, current path is: " + pathMatcher
                        + " ,and service method is: "
                        + invokerRestMethodMetadataPair.getRestMethodMetadata().getReflectMethod()
                        + "before service  method is: "
                        + beforeMetadata.getRestMethodMetadata().getReflectMethod());
            }
        }

        pathMatcherPairMap.put(pathMatcher, invokerRestMethodMetadataPair);

        addPathMatcherToHttpMethodsMap(pathMatcher);

        logger.info("dubbo rest deploy pathMatcher:" + pathMatcher + ", and service method is :"
                + invokerRestMethodMetadataPair.getRestMethodMetadata().getReflectMethod());
    }

    private void addPathMatcherToHttpMethodsMap(PathMatcher pathMatcher) {

        PathMatcher newPathMatcher = PathMatcher.convertPathMatcher(pathMatcher);

        Set<String> httpMethods = pathMatcherToHttpMethodMap.computeIfAbsent(newPathMatcher, k -> {
            HashSet<String> methods = new HashSet<>();

            methods.add(pathMatcher.getHttpMethod());
            return methods;
        });

        httpMethods.add(newPathMatcher.getHttpMethod());
    }

    public boolean isHttpMethodAllowed(PathMatcher pathMatcher) {

        PathMatcher newPathMatcher = PathMatcher.convertPathMatcher(pathMatcher);
        if (!pathMatcherToHttpMethodMap.containsKey(newPathMatcher)) {
            return false;
        }

        Set<String> httpMethods = pathMatcherToHttpMethodMap.get(newPathMatcher);

        return httpMethods.contains(newPathMatcher.getHttpMethod());
    }

    public String pathHttpMethods(PathMatcher pathMatcher) {

        PathMatcher newPathMatcher = PathMatcher.convertPathMatcher(pathMatcher);
        if (!pathMatcherToHttpMethodMap.containsKey(newPathMatcher)) {
            return null;
        }

        Set<String> httpMethods = pathMatcherToHttpMethodMap.get(newPathMatcher);

        return httpMethods.toString();
    }
}
