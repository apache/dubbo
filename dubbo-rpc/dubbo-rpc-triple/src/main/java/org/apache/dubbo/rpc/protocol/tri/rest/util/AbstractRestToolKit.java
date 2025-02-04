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
package org.apache.dubbo.rpc.protocol.tri.rest.util;

import org.apache.dubbo.common.utils.AnnotationUtils;
import org.apache.dubbo.common.utils.DefaultParameterNameReader;
import org.apache.dubbo.common.utils.ParameterNameReader;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.Messages;
import org.apache.dubbo.rpc.protocol.tri.rest.RestException;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.CompositeArgumentResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.GeneralTypeConverter;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.TypeConverter;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.NamedValueMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;

import javax.annotation.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;

public abstract class AbstractRestToolKit implements RestToolKit {

    protected final FrameworkModel frameworkModel;
    protected final TypeConverter typeConverter;
    protected final ParameterNameReader parameterNameReader;
    protected final CompositeArgumentResolver argumentResolver;

    public AbstractRestToolKit(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
        typeConverter = frameworkModel.getOrRegisterBean(GeneralTypeConverter.class);
        parameterNameReader = frameworkModel.getOrRegisterBean(DefaultParameterNameReader.class);
        argumentResolver = frameworkModel.getOrRegisterBean(CompositeArgumentResolver.class);
    }

    @Override
    public String resolvePlaceholders(String text) {
        return RestUtils.replacePlaceholder(text, k -> frameworkModel
                .defaultApplication()
                .modelEnvironment()
                .getConfiguration()
                .getString(k));
    }

    @Override
    public Object convert(Object value, ParameterMeta parameter) {
        Object target = typeConverter.convert(value, parameter.getGenericType());
        if (target == null && value != null) {
            throw new RestException(
                    Messages.ARGUMENT_CONVERT_ERROR,
                    parameter.getName(),
                    value,
                    value.getClass(),
                    parameter.getGenericType());
        }
        return target;
    }

    @Override
    public NamedValueMeta getNamedValueMeta(ParameterMeta parameter) {
        return argumentResolver.getNamedValueMeta(parameter);
    }

    @Override
    public String[] getParameterNames(Method method) {
        return parameterNameReader.readParameterNames(method);
    }

    @Nullable
    @Override
    public String[] getParameterNames(Constructor<?> ctor) {
        return parameterNameReader.readParameterNames(ctor);
    }

    @Override
    public Map<String, Object> getAttributes(AnnotatedElement element, Annotation annotation) {
        return AnnotationUtils.getAttributes(annotation, false);
    }
}
