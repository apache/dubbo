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
package org.apache.dubbo.rpc.protocol.rest.deploy;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.rest.PathMatcher;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.metadata.rest.ServiceRestMetadata;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.protocol.rest.Constants;
import org.apache.dubbo.rpc.protocol.rest.PathAndInvokerMapper;
import org.apache.dubbo.rpc.protocol.rest.RpcExceptionMapper;
import org.apache.dubbo.rpc.protocol.rest.exception.mapper.ExceptionMapper;

import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SPLIT_PATTERN;

public class ServiceDeployer {

    private final PathAndInvokerMapper pathAndInvokerMapper = new PathAndInvokerMapper();
    private final ExceptionMapper exceptionMapper = new ExceptionMapper();


    public void deploy(ServiceRestMetadata serviceRestMetadata, Invoker invoker) {
        Map<PathMatcher, RestMethodMetadata> pathToServiceMapContainPathVariable =
            serviceRestMetadata.getPathContainPathVariableToServiceMap();
        pathAndInvokerMapper.addPathAndInvoker(pathToServiceMapContainPathVariable, invoker);

        Map<PathMatcher, RestMethodMetadata> pathToServiceMapUnContainPathVariable =
            serviceRestMetadata.getPathUnContainPathVariableToServiceMap();
        pathAndInvokerMapper.addPathAndInvoker(pathToServiceMapUnContainPathVariable, invoker);
    }

    public void undeploy(ServiceRestMetadata serviceRestMetadata) {
        Map<PathMatcher, RestMethodMetadata> pathToServiceMapContainPathVariable =
            serviceRestMetadata.getPathContainPathVariableToServiceMap();
        pathToServiceMapContainPathVariable.keySet().stream().forEach(pathAndInvokerMapper::removePath);

        Map<PathMatcher, RestMethodMetadata> pathToServiceMapUnContainPathVariable =
            serviceRestMetadata.getPathUnContainPathVariableToServiceMap();
        pathToServiceMapUnContainPathVariable.keySet().stream().forEach(pathAndInvokerMapper::removePath);

    }

    public void registerExceptionMapper(URL url) {

        for (String clazz : COMMA_SPLIT_PATTERN.split(url.getParameter(Constants.EXTENSION_KEY, RpcExceptionMapper.class.getName()))) {
            if (!StringUtils.isEmpty(clazz)) {
                exceptionMapper.registerMapper(clazz);
            }
        }
    }

    public PathAndInvokerMapper getPathAndInvokerMapper() {
        return pathAndInvokerMapper;
    }

    public ExceptionMapper getExceptionMapper() {
        return exceptionMapper;
    }
}
