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

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.cors.CorsMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RequestMapping;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RequestMapping.Builder;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RequestMappingResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.ServiceVersionCondition;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.AnnotationMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.MethodMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ServiceMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RestToolKit;

import java.util.Arrays;

import org.springframework.http.HttpStatus;

@Activate(onClass = "org.springframework.web.bind.annotation.RequestMapping")
public class SpringMvcRequestMappingResolver implements RequestMappingResolver {

    private final FrameworkModel frameworkModel;
    private volatile RestToolKit toolKit;

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
        return builder(requestMapping, responseStatus)
                .name(serviceMeta.getType().getSimpleName())
                .contextPath(serviceMeta.getContextPath())
                .cors(createCorsMeta(crossOrigin))
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
        return builder(requestMapping, responseStatus)
                .name(methodMeta.getMethod().getName())
                .contextPath(serviceMeta.getContextPath())
                .custom(new ServiceVersionCondition(serviceMeta.getServiceGroup(), serviceMeta.getServiceVersion()))
                .cors(createCorsMeta(crossOrigin))
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
                .method(requestMapping.getStringArray("method"))
                .param(requestMapping.getStringArray("params"))
                .header(requestMapping.getStringArray("headers"))
                .consume(requestMapping.getStringArray("consumes"))
                .produce(requestMapping.getStringArray("produces"));
    }

    private CorsMeta createCorsMeta(AnnotationMeta<?> crossOrigin) {
        CorsMeta meta = new CorsMeta();
        if (crossOrigin == null) {
            return meta;
        }
        meta.setAllowCredentials(crossOrigin.getBoolean("allowCredentials"));
        meta.setAllowedHeaders(Arrays.asList(crossOrigin.getStringArray("allowedHeaders")));
        meta.setAllowedMethods(Arrays.asList(crossOrigin.getStringArray("allowedMethods")));
        meta.setAllowedOrigins(Arrays.asList(crossOrigin.getStringArray("allowedOrigins")));
        meta.setExposedHeaders(Arrays.asList(crossOrigin.getStringArray("exposedHeaders")));
        meta.setMaxAge(crossOrigin.getNumber("maxAge").longValue());
        return meta;
    }
}
