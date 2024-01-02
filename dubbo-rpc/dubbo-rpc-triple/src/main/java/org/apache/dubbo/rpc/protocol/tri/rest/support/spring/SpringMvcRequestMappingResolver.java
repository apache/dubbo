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
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RequestMapping;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RequestMappingResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RestToolKit;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.AnnotationMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.MethodMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ServiceMeta;

@Activate
public class SpringMvcRequestMappingResolver implements RequestMappingResolver {

    private final RestToolKit toolKit;

    public SpringMvcRequestMappingResolver(FrameworkModel frameworkModel) {
        toolKit = frameworkModel.getActivateExtensions(RestToolKit.class).get(0);
    }

    @Override
    public RestToolKit getRestToolKit() {
        return toolKit;
    }

    @Override
    public RequestMapping resolve(ServiceMeta serviceMeta) {
        AnnotationMeta<?> requestMapping = serviceMeta.findMergedAnnotation(Annotations.RequestMapping);
        if (requestMapping == null) {
            return null;
        }
        return build(serviceMeta.getType().getSimpleName(), requestMapping);
    }

    @Override
    public RequestMapping resolve(MethodMeta methodMeta) {
        AnnotationMeta<?> requestMapping = methodMeta.findMergedAnnotation(Annotations.RequestMapping);
        if (requestMapping == null) {
            return null;
        }
        return build(methodMeta.getMethod().getName(), requestMapping);
    }

    private RequestMapping build(String name, AnnotationMeta<?> requestMapping) {
        return RequestMapping.builder()
                .name(name)
                .path(requestMapping.getValueArray())
                .method(requestMapping.getStringArray("method"))
                .param(requestMapping.getStringArray("params"))
                .header(requestMapping.getStringArray("headers"))
                .consume(requestMapping.getStringArray("consumes"))
                .produce(requestMapping.getStringArray("produces"))
                .build();
    }
}
