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

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.Pair;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.rpc.protocol.tri.rest.Messages;
import org.apache.dubbo.rpc.protocol.tri.rest.RestException;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.ArgumentResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.CompositeArgumentResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.AnnotationMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.BeanMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.BeanMeta.ConstructorMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.BeanMeta.PropertyMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.util.TypeUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class BeanArgumentBinder {

    private static final ThreadLocal<Set<Class<?>>> LOCAL = new ThreadLocal<>();

    private final Map<Pair<Class<?>, String>, BeanMeta> cache = CollectionUtils.newConcurrentHashMap();
    private final ArgumentResolver argumentResolver;

    BeanArgumentBinder(CompositeArgumentResolver argumentResolver) {
        this.argumentResolver = argumentResolver;
    }

    public Object bind(ParameterMeta paramMeta, HttpRequest request, HttpResponse response) {
        Set<Class<?>> walked = LOCAL.get();
        if (walked == null) {
            LOCAL.set(new HashSet<>());
        }
        try {
            return resolveArgument(paramMeta, request, response);
        } catch (Exception e) {
            throw new RestException(e, Messages.ARGUMENT_BIND_ERROR, paramMeta.getName(), paramMeta.getType());
        } finally {
            if (walked == null) {
                LOCAL.remove();
            }
        }
    }

    private Object resolveArgument(ParameterMeta paramMeta, HttpRequest request, HttpResponse response) {
        if (paramMeta.isSimple()) {
            return argumentResolver.resolve(paramMeta, request, response);
        }

        // Prevent infinite loops
        if (LOCAL.get().add(paramMeta.getActualType())) {
            AnnotationMeta<?> form = paramMeta.findAnnotation(Annotations.Form);
            if (form != null || paramMeta.isHierarchyAnnotated(Annotations.BeanParam)) {
                String prefix = form == null ? null : form.getString("prefix");
                BeanMeta beanMeta = cache.computeIfAbsent(
                        Pair.of(paramMeta.getActualType(), prefix),
                        k -> new BeanMeta(paramMeta.getToolKit(), k.getValue(), k.getKey()));

                ConstructorMeta constructor = beanMeta.getConstructor();
                ParameterMeta[] parameters = constructor.getParameters();
                Object bean;
                int len = parameters.length;
                if (len == 0) {
                    bean = constructor.newInstance();
                } else {
                    Object[] args = new Object[len];
                    for (int i = 0; i < len; i++) {
                        args[i] = resolveArgument(parameters[i], request, response);
                    }
                    bean = constructor.newInstance(args);
                }

                for (PropertyMeta propertyMeta : beanMeta.getProperties()) {
                    if (propertyMeta.canSetValue()) {
                        propertyMeta.setValue(bean, resolveArgument(propertyMeta, request, response));
                    }
                }

                return bean;
            }
        }

        return TypeUtils.nullDefault(paramMeta.getType());
    }
}
