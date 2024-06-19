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
package org.apache.dubbo.rpc.protocol.tri.rest.support.basic;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.protocol.tri.rest.cors.CorsUtils;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RequestMapping;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RequestMapping.Builder;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RequestMappingResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.ServiceVersionCondition;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.AnnotationMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.CorsMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.MethodMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ServiceMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.util.DefaultRestToolKit;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RestToolKit;
import org.apache.dubbo.rpc.protocol.tri.rest.util.TypeUtils;

import java.lang.reflect.Method;

@Activate
public class BasicRequestMappingResolver implements RequestMappingResolver {

    private final FrameworkModel frameworkModel;
    private final RestToolKit toolKit;
    private CorsMeta globalCorsMeta;

    public BasicRequestMappingResolver(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
        toolKit = new DefaultRestToolKit(frameworkModel);
    }

    @Override
    public RestToolKit getRestToolKit() {
        return toolKit;
    }

    @Override
    public boolean accept(MethodMeta methodMeta, MethodDescriptor methodDescriptor) {
        return methodDescriptor != null || methodMeta.findAnnotation(Annotations.Mapping) != null;
    }

    @Override
    public RequestMapping resolve(ServiceMeta serviceMeta) {
        AnnotationMeta<?> mapping = serviceMeta.findAnnotation(Annotations.Mapping);
        Builder builder = builder(mapping);

        String[] paths = getPaths(mapping);
        if (paths.length == 0) {
            builder.path(serviceMeta.getServiceInterface());
        } else {
            builder.path(paths);
        }

        return builder.name(serviceMeta.getType().getSimpleName())
                .contextPath(serviceMeta.getContextPath())
                .build();
    }

    @Override
    public RequestMapping resolve(MethodMeta methodMeta) {
        Method method = methodMeta.getMethod();
        AnnotationMeta<?> mapping = methodMeta.findAnnotation(Annotations.Mapping);
        Builder builder = builder(mapping);

        String[] paths = getPaths(mapping);
        if (paths.length == 0) {
            builder.path(method.getName()).sig(TypeUtils.buildSig(method));
        } else {
            builder.path(paths);
        }

        ServiceMeta serviceMeta = methodMeta.getServiceMeta();
        if (globalCorsMeta == null) {
            globalCorsMeta = CorsUtils.getGlobalCorsMeta(frameworkModel);
        }
        return builder.name(method.getName())
                .custom(new ServiceVersionCondition(serviceMeta.getServiceGroup(), serviceMeta.getServiceVersion()))
                .cors(globalCorsMeta)
                .build();
    }

    private Builder builder(AnnotationMeta<?> mapping) {
        Builder builder = RequestMapping.builder();
        if (mapping == null) {
            return builder;
        }
        builder.method(mapping.getStringArray("method"))
                .param(mapping.getStringArray("params"))
                .header(mapping.getStringArray("headers"))
                .consume(mapping.getStringArray("consumes"))
                .produce(mapping.getStringArray("produces"));
        return builder;
    }

    private static String[] getPaths(AnnotationMeta<?> mapping) {
        if (mapping == null) {
            return StringUtils.EMPTY_STRING_ARRAY;
        }
        String[] paths = mapping.getStringArray("path");
        if (paths.length > 0) {
            return paths;
        }
        return mapping.getValueArray();
    }
}
