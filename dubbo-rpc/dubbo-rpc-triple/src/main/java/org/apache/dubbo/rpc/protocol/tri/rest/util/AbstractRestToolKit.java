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

import org.apache.dubbo.common.config.Environment;
import org.apache.dubbo.common.utils.AnnotationUtils;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.Messages;
import org.apache.dubbo.rpc.protocol.tri.rest.RestException;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.GeneralTypeConverter;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.TypeConverter;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

public abstract class AbstractRestToolKit implements RestToolKit {

    protected final FrameworkModel frameworkModel;
    protected final TypeConverter typeConverter;

    public AbstractRestToolKit(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
        typeConverter = frameworkModel.getBeanFactory().getOrRegisterBean(GeneralTypeConverter.class);
    }

    @Override
    public String resolvePlaceholders(String text) {
        return RestUtils.hasPlaceholder(text) ? getEnvironment().resolvePlaceholders(text) : text;
    }

    private Environment getEnvironment() {
        return frameworkModel.defaultApplication().modelEnvironment();
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
    public String[] getParameterNames(Method method) {
        Parameter[] parameters = method.getParameters();
        int len = parameters.length;
        String[] names = new String[len];
        for (int i = 0; i < len; i++) {
            Parameter param = parameters[i];
            if (!param.isNamePresent()) {
                return null;
            }
            names[i] = param.getName();
        }
        return names;
    }

    @Override
    public Map<String, Object> getAttributes(AnnotatedElement element, Annotation annotation) {
        return AnnotationUtils.getAttributes(annotation, false);
    }
}
