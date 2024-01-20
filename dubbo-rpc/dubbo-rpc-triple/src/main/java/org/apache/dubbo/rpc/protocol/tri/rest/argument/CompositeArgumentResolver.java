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
package org.apache.dubbo.rpc.protocol.tri.rest.argument;

import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.AnnotationMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"rawtypes", "unchecked"})
public class CompositeArgumentResolver implements ArgumentResolver {

    private final Map<Class, AnnotationBaseArgumentResolver> resolverMap = new HashMap<>();
    private final ArgumentResolver[] resolvers;

    public CompositeArgumentResolver(FrameworkModel frameworkModel) {
        List<ArgumentResolver> extensions = frameworkModel.getActivateExtensions(ArgumentResolver.class);
        List<ArgumentResolver> resolvers = new ArrayList<>(extensions.size());
        for (ArgumentResolver resolver : extensions) {
            if (resolver instanceof AnnotationBaseArgumentResolver) {
                AnnotationBaseArgumentResolver aar = (AnnotationBaseArgumentResolver) resolver;
                resolverMap.put(aar.accept(), aar);
            } else {
                resolvers.add(resolver);
            }
        }
        this.resolvers = resolvers.toArray(new ArgumentResolver[0]);
    }

    @Override
    public boolean accept(ParameterMeta parameter) {
        return true;
    }

    @Override
    public Object resolve(ParameterMeta parameter, HttpRequest request, HttpResponse response) {
        AnnotationMeta[] annotations = parameter.findAnnotations();
        for (AnnotationMeta annotation : annotations) {
            AnnotationBaseArgumentResolver resolver = resolverMap.get(annotation.getClass());
            if (resolver != null) {
                return resolver.resolve(parameter, annotation, request, response);
            }
        }
        for (ArgumentResolver resolver : resolvers) {
            if (resolver.accept(parameter)) {
                return resolver.resolve(parameter, request, response);
            }
        }

        return request.parameter(parameter.getName());
    }
}
