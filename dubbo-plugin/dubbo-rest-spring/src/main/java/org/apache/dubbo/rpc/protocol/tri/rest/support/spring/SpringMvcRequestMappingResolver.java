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
package org.apache.dubbo.rpc.protocol.tri.rest.support.spring;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.cors.CorsUtils;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RequestMapping;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RequestMapping.Builder;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RequestMappingResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.ServiceVersionCondition;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.AnnotationMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.CorsMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.MethodMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ServiceMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RestToolKit;

import org.springframework.http.HttpStatus;

@Activate(onClass = "org.springframework.web.bind.annotation.RequestMapping")
public class SpringMvcRequestMappingResolver implements RequestMappingResolver {

    private final FrameworkModel frameworkModel;
    private volatile RestToolKit toolKit;
    private CorsMeta globalCorsMeta;

    public SpringMvcRequestMappingResolver(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    @Override
    public RestToolKit getRestToolKit() {
        RestToolKit toolKit = this.toolKit;
        if (toolKit == null) {
            synchronized (this) {
                toolKit = this.toolKit;
                if (toolKit == null) {
                    toolKit = new SpringRestToolKit(frameworkModel);
                    this.toolKit = toolKit;
                }
            }
        }
        return toolKit;
    }

    @Override
    public RequestMapping resolve(ServiceMeta serviceMeta) {
        AnnotationMeta<?> requestMapping = serviceMeta.findMergedAnnotation(Annotations.RequestMapping);
        if (requestMapping == null) {
            return null;
        }
        AnnotationMeta<?> responseStatus = serviceMeta.findMergedAnnotation(Annotations.ResponseStatus);
        AnnotationMeta<?> crossOrigin = serviceMeta.findMergedAnnotation(Annotations.CrossOrigin);
        String[] methods = requestMapping.getStringArray("method");
        return builder(requestMapping, responseStatus)
                .method(methods)
                .name(serviceMeta.getType().getSimpleName())
                .contextPath(serviceMeta.getContextPath())
                .cors(buildCorsMeta(crossOrigin, methods))
                .build();
    }

    @Override
    public RequestMapping resolve(MethodMeta methodMeta) {
        AnnotationMeta<?> requestMapping = methodMeta.findMergedAnnotation(Annotations.RequestMapping);
        if (requestMapping == null) {
            AnnotationMeta<?> exceptionHandler = methodMeta.getAnnotation(Annotations.ExceptionHandler);
            if (exceptionHandler != null) {
                methodMeta.getServiceMeta().addExceptionHandler(methodMeta);
            }
            return null;
        }
        ServiceMeta serviceMeta = methodMeta.getServiceMeta();
        AnnotationMeta<?> responseStatus = methodMeta.findMergedAnnotation(Annotations.ResponseStatus);
        AnnotationMeta<?> crossOrigin = methodMeta.findMergedAnnotation(Annotations.CrossOrigin);
        String[] methods = requestMapping.getStringArray("method");
        return builder(requestMapping, responseStatus)
                .method(methods)
                .name(methodMeta.getMethod().getName())
                .contextPath(serviceMeta.getContextPath())
                .custom(new ServiceVersionCondition(serviceMeta.getServiceGroup(), serviceMeta.getServiceVersion()))
                .cors(buildCorsMeta(crossOrigin, methods))
                .build();
    }

    private Builder builder(AnnotationMeta<?> requestMapping, AnnotationMeta<?> responseStatus) {
        Builder builder = RequestMapping.builder();
        if (responseStatus != null) {
            HttpStatus value = responseStatus.getEnum("value");
            builder.responseStatus(value.value());
            String reason = responseStatus.getString("reason");
            if (StringUtils.isNotEmpty(reason)) {
                builder.responseReason(reason);
            }
        }
        return builder.path(requestMapping.getValueArray())
                .param(requestMapping.getStringArray("params"))
                .header(requestMapping.getStringArray("headers"))
                .consume(requestMapping.getStringArray("consumes"))
                .produce(requestMapping.getStringArray("produces"));
    }

    private CorsMeta buildCorsMeta(AnnotationMeta<?> crossOrigin, String[] methods) {
        if (globalCorsMeta == null) {
            globalCorsMeta = CorsUtils.getGlobalCorsMeta(frameworkModel);
        }
        if (crossOrigin == null) {
            return globalCorsMeta;
        }
        String[] allowedMethods = crossOrigin.getStringArray("methods");
        if (allowedMethods.length == 0) {
            allowedMethods = methods;
            if (allowedMethods.length == 0) {
                allowedMethods = new String[] {CommonConstants.ANY_VALUE};
            }
        }
        CorsMeta corsMeta = CorsMeta.builder()
                .allowedOrigins(crossOrigin.getStringArray("origins"))
                .allowedMethods(allowedMethods)
                .allowedHeaders(crossOrigin.getStringArray("allowedHeaders"))
                .exposedHeaders(crossOrigin.getStringArray("exposedHeaders"))
                .allowCredentials(crossOrigin.getString("allowCredentials"))
                .maxAge(crossOrigin.getNumber("maxAge"))
                .build();
        return globalCorsMeta.combine(corsMeta);
    }
}
