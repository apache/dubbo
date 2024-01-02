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
package org.apache.dubbo.rpc.protocol.tri.rest.support.jaxrs;

import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RequestMapping;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RequestMappingResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RestToolKit;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.AnnotationMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.AnnotationSupport;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.MethodMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ServiceMeta;

public class JAXRSRequestMappingResolver implements RequestMappingResolver {

    private final RestToolKit toolKit;

    public JAXRSRequestMappingResolver(FrameworkModel frameworkModel) {
        toolKit = frameworkModel.getActivateExtensions(RestToolKit.class).get(0);
    }

    @Override
    public RestToolKit getRestToolKit() {
        return toolKit;
    }

    @Override
    public RequestMapping resolve(ServiceMeta serviceMeta) {
        AnnotationMeta<?> path = serviceMeta.findAnnotation(Annotations.Path);
        if (path == null) {
            return null;
        }
        return build(serviceMeta, serviceMeta.getType().getSimpleName(), path, null);
    }

    @Override
    public RequestMapping resolve(MethodMeta methodMeta) {
        AnnotationMeta<?> path = methodMeta.findAnnotation(Annotations.Path);
        if (path == null) {
            return null;
        }
        AnnotationMeta<?> httpMethod = methodMeta.findAnnotation(Annotations.HttpMethod);
        if (httpMethod == null) {
            return null;
        }
        return build(methodMeta, methodMeta.getMethod().getName(), path, httpMethod);
    }

    private RequestMapping build(
            AnnotationSupport meta, String name, AnnotationMeta<?> path, AnnotationMeta<?> httpMethod) {
        RequestMapping.Builder builder = RequestMapping.builder().name(name).path(path.getValue());
        if (httpMethod == null) {
            httpMethod = meta.findAnnotation(Annotations.HttpMethod);
        }
        if (httpMethod != null) {
            builder.method(httpMethod.getValue());
        }
        AnnotationMeta<?> produces = meta.findAnnotation(Annotations.Produces);
        if (produces != null) {
            builder.produce(produces.getValueArray());
        }
        AnnotationMeta<?> consumes = meta.findAnnotation(Annotations.Consumes);
        if (consumes != null) {
            builder.consume(consumes.getValueArray());
        }
        return builder.build();
    }
}
