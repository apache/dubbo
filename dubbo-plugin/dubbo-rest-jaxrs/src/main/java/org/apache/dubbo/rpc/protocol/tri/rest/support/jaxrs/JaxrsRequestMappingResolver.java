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

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.config.nested.RestConfig;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.cors.CorsUtils;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RequestMapping;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RequestMapping.Builder;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RequestMappingResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.AnnotationMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.AnnotationSupport;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.CorsMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.MethodMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ServiceMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RestToolKit;

@Activate(onClass = {"javax.ws.rs.Path", "javax.ws.rs.container.ContainerRequestContext"})
public class JaxrsRequestMappingResolver implements RequestMappingResolver {

    private final RestToolKit toolKit;
    private RestConfig restConfig;
    private CorsMeta globalCorsMeta;

    public JaxrsRequestMappingResolver(FrameworkModel frameworkModel) {
        toolKit = new JaxrsRestToolKit(frameworkModel);
    }

    @Override
    public RestToolKit getRestToolKit() {
        return toolKit;
    }

    @Override
    public void setRestConfig(RestConfig restConfig) {
        this.restConfig = restConfig;
    }

    @Override
    public RequestMapping resolve(ServiceMeta serviceMeta) {
        AnnotationMeta<?> path = serviceMeta.findAnnotation(Annotations.Path);
        if (path == null) {
            return null;
        }
        return builder(serviceMeta, path, null)
                .name(serviceMeta.getType().getSimpleName())
                .contextPath(serviceMeta.getContextPath())
                .build();
    }

    @Override
    public RequestMapping resolve(MethodMeta methodMeta) {
        AnnotationMeta<?> path = methodMeta.findAnnotation(Annotations.Path);
        if (path == null) {
            return null;
        }
        AnnotationMeta<?> httpMethod = methodMeta.findMergedAnnotation(Annotations.HttpMethod);
        if (httpMethod == null) {
            return null;
        }
        ServiceMeta serviceMeta = methodMeta.getServiceMeta();
        if (globalCorsMeta == null) {
            globalCorsMeta = CorsUtils.getGlobalCorsMeta(restConfig);
        }
        return builder(methodMeta, path, httpMethod)
                .name(methodMeta.getMethod().getName())
                .contextPath(methodMeta.getServiceMeta().getContextPath())
                .service(serviceMeta.getServiceGroup(), serviceMeta.getServiceVersion())
                .cors(globalCorsMeta)
                .build();
    }

    private Builder builder(AnnotationSupport meta, AnnotationMeta<?> path, AnnotationMeta<?> httpMethod) {
        Builder builder = RequestMapping.builder().path(path.getValue());
        if (httpMethod == null) {
            httpMethod = meta.findMergedAnnotation(Annotations.HttpMethod);
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
        return builder;
    }
}
