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
    public RequestMapping resolve(ServiceMeta serviceMeta) {
        return builder(serviceMeta.findAnnotation(Annotations.Mapping), serviceMeta.getServiceInterface())
                .name(serviceMeta.getType().getSimpleName())
                .contextPath(serviceMeta.getContextPath())
                .build();
    }

    @Override
    public RequestMapping resolve(MethodMeta methodMeta) {
        ServiceMeta serviceMeta = methodMeta.getServiceMeta();
        if (globalCorsMeta == null) {
            globalCorsMeta = CorsUtils.getGlobalCorsMeta(frameworkModel);
        }
        String name = methodMeta.getMethod().getName();
        return builder(methodMeta.findAnnotation(Annotations.Mapping), name)
                .name(name)
                .custom(new ServiceVersionCondition(serviceMeta.getServiceGroup(), serviceMeta.getServiceVersion()))
                .cors(globalCorsMeta)
                .build();
    }

    private Builder builder(AnnotationMeta<?> mapping, String defaultPath) {
        Builder builder = RequestMapping.builder();
        String[] paths = StringUtils.EMPTY_STRING_ARRAY;
        if (mapping != null) {
            builder.method(mapping.getStringArray("method"))
                    .param(mapping.getStringArray("params"))
                    .header(mapping.getStringArray("headers"))
                    .consume(mapping.getStringArray("consumes"))
                    .produce(mapping.getStringArray("produces"));

            paths = mapping.getStringArray("path");
            if (paths.length == 0) {
                paths = mapping.getValueArray();
            }
        }
        return paths.length == 0 ? builder.path(defaultPath) : builder.path(paths);
    }
}
