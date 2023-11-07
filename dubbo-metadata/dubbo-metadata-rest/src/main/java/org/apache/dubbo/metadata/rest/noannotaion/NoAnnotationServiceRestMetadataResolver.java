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
package org.apache.dubbo.metadata.rest.noannotaion;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.metadata.rest.AbstractServiceRestMetadataResolver;
import org.apache.dubbo.metadata.rest.ArgInfo;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.metadata.rest.media.MediaType;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Set;

import static org.apache.dubbo.common.utils.PathUtils.buildPath;

/**
 * NoAnnotationServiceRestMetadataResolver
 *
 * @since 3.3
 */
@Activate(order = Integer.MAX_VALUE)
public class NoAnnotationServiceRestMetadataResolver extends AbstractServiceRestMetadataResolver {

    private static final String CONTENT_TYPE = MediaType.APPLICATION_JSON_VALUE.value;
    private static final String REQUEST_METHOD = "POST";

    public NoAnnotationServiceRestMetadataResolver(ApplicationModel applicationModel) {
        super(applicationModel);
    }

    @Override
    protected boolean supports0(Class<?> serviceType) {
        // class @Controller or @RequestMapping
        return true;
    }

    @Override
    protected boolean isRestCapableMethod(Method serviceMethod, Class<?> serviceType, Class<?> serviceInterfaceClass) {
        // method only match @RequestMapping
        return true;
    }

    @Override
    protected String resolveRequestMethod(Method serviceMethod, Class<?> serviceType, Class<?> serviceInterfaceClass) {

        return REQUEST_METHOD;
    }

    @Override
    protected String resolveRequestPath(Method serviceMethod, Class<?> serviceType, Class<?> serviceInterfaceClass) {

        // use serviceInterfaceClass class name
        return buildPath(serviceInterfaceClass.getName(), serviceMethod.getName());
    }

    @Override
    protected void processProduces(
            Method serviceMethod, Class<?> serviceType, Class<?> serviceInterfaceClass, Set<String> produces) {
        produces.add(CONTENT_TYPE);
    }

    @Override
    protected void processConsumes(
            Method serviceMethod, Class<?> serviceType, Class<?> serviceInterfaceClass, Set<String> consumes) {
        consumes.add(CONTENT_TYPE);
    }

    @Override
    protected void processAnnotatedMethodParameter(
            Parameter parameter,
            int parameterIndex,
            Method serviceMethod,
            Class<?> serviceType,
            Class<?> serviceInterfaceClass,
            RestMethodMetadata metadata) {
        ArgInfo argInfo = ArgInfo.build(parameterIndex, parameter);
        metadata.addArgInfo(argInfo);
    }
}
