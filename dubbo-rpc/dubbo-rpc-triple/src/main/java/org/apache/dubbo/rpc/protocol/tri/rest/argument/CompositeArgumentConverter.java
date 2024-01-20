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

import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;

import java.util.List;

@SuppressWarnings({"rawtypes", "unchecked"})
public final class CompositeArgumentConverter implements ArgumentConverter {

    private final List<ArgumentConverter> converters;
    private final TypeConverter typeConverter;

    public CompositeArgumentConverter(FrameworkModel frameworkModel) {
        converters = frameworkModel.getActivateExtensions(ArgumentConverter.class);
        typeConverter = frameworkModel.getBeanFactory().getBean(TypeConverter.class);
    }

    @Override
    public Object convert(Object value, Class targetType, ParameterMeta parameter) {
        if (value == null) {
            return typeConverter.convert(null, targetType);
        }
        if (targetType.isInstance(value)) {
            if (parameter.getGenericType() instanceof Class) {
                return value;
            }
            return typeConverter.convert(value, parameter.getGenericType());
        }

        Object target;
        for (ArgumentConverter converter : converters) {
            target = converter.convert(value, targetType, parameter);
            if (target != null) {
                return target;
            }
        }
        target = parameter.getToolKit().convert(value, parameter);
        if (target != null) {
            return target;
        }
        return typeConverter.convert(value, parameter.getGenericType());
    }
}
