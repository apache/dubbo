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
package org.apache.dubbo.rpc.protocol.tri.rest.mapping;

import org.apache.dubbo.common.config.Environment;
import org.apache.dubbo.common.convert.ConverterUtil;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.AnnotationUtils;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.RestUtils;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

@Activate(order = 10000)
public final class DefaultRestToolKit implements RestToolKit {

    private final Environment environment;
    private final ConverterUtil converterUtil;

    public DefaultRestToolKit(FrameworkModel frameworkModel) {
        environment = frameworkModel.defaultApplication().modelEnvironment();
        converterUtil = frameworkModel.getBeanFactory().getBean(ConverterUtil.class);
    }

    @Override
    public String resolvePlaceholders(String text) {
        return RestUtils.hasPlaceholder(text) ? environment.resolvePlaceholders(text) : text;
    }

    @Override
    public Object convert(Object value, ParameterMeta parameter) {
        return converterUtil.convertIfPossible(value, parameter.getType());
    }

    @Override
    public String[] getParameterNames(Method method) {
        String[] names = new String[method.getParameterCount()];
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < names.length; i++) {
            names[i] = parameters[i].getName();
        }
        return names;
    }

    @Override
    public Map<String, Object> getAttributes(AnnotatedElement element, Annotation annotation) {
        return AnnotationUtils.getAttributes(annotation, false);
    }
}
