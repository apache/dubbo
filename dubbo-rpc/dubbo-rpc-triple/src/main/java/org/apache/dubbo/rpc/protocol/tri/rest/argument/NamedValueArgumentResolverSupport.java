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

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.rpc.protocol.tri.rest.Messages;
import org.apache.dubbo.rpc.protocol.tri.rest.RestParameterException;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.NamedValueMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.util.TypeUtils;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public abstract class NamedValueArgumentResolverSupport {

    protected final Map<ParameterMeta, NamedValueMeta> cache = CollectionUtils.newConcurrentHashMap();

    protected final Object resolve(NamedValueMeta meta, HttpRequest request, HttpResponse response) {
        Class<?> type = meta.type();
        Object arg;
        if (type.isArray() || Collection.class.isAssignableFrom(type)) {
            arg = resolveCollectionValue(meta, request, response);
        } else if (Map.class.isAssignableFrom(type)) {
            arg = resolveMapValue(meta, request, response);
        } else {
            arg = resolveValue(meta, request, response);
        }

        if (arg != null) {
            return StringUtils.EMPTY_STRING.equals(arg) ? meta.defaultValue() : arg;
        }
        arg = meta.defaultValue();
        if (arg != null) {
            return arg;
        }
        if (meta.required()) {
            throw new RestParameterException(Messages.ARGUMENT_VALUE_MISSING, meta.name(), type);
        }
        return null;
    }

    protected final NamedValueMeta updateNamedValueMeta(ParameterMeta parameterMeta, NamedValueMeta meta) {
        if (StringUtils.isEmpty(meta.name())) {
            meta.setName(parameterMeta.getRequiredName());
        }

        Class<?> type = parameterMeta.getType();
        Type genericType = parameterMeta.getGenericType();
        if (type == Optional.class) {
            Class<?> actualType = TypeUtils.getNestedType(genericType, 0);
            meta.setType(actualType == null ? Object.class : actualType);
        } else {
            meta.setType(type);
        }
        if (type.isArray()) {
            meta.setNestedTypes(new Class<?>[] {type.getComponentType()});
        } else {
            meta.setNestedTypes(TypeUtils.getNestedTypes(genericType));
        }
        meta.setParameterMeta(parameterMeta);
        return meta;
    }

    protected abstract Object resolveValue(NamedValueMeta meta, HttpRequest request, HttpResponse response);

    protected Object resolveCollectionValue(NamedValueMeta meta, HttpRequest request, HttpResponse response) {
        return resolveValue(meta, request, response);
    }

    protected Object resolveMapValue(NamedValueMeta meta, HttpRequest request, HttpResponse response) {
        Object value = resolveValue(meta, request, response);
        return value instanceof Map ? value : Collections.singletonMap(meta.name(), value);
    }
}
