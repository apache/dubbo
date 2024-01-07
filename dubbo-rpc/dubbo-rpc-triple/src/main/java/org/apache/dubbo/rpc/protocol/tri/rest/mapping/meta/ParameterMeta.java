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
package org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta;

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RestToolKit;
import org.apache.dubbo.rpc.protocol.tri.rest.util.TypeUtils;

import java.lang.reflect.Type;
import java.util.Optional;

public abstract class ParameterMeta extends AnnotationSupport {

    private final String prefix;
    private final String name;
    private Boolean simple;
    private Class<?> actualType;
    private Object typeDescriptor;

    protected ParameterMeta(RestToolKit toolKit, String prefix, String name) {
        super(toolKit);
        this.prefix = StringUtils.isEmpty(prefix) ? null : prefix;
        this.name = name;
    }

    protected ParameterMeta(RestToolKit toolKit, String name) {
        super(toolKit);
        prefix = null;
        this.name = name;
    }

    public String getPrefix() {
        return prefix;
    }

    public final String getName() {
        return name;
    }

    public final boolean isSimple() {
        if (simple == null) {
            simple = TypeUtils.isSimpleProperty(getActualType());
        }
        return simple;
    }

    public final Class<?> getActualType() {
        if (actualType == null) {
            Class<?> type = getType();
            if (type == Optional.class) {
                type = TypeUtils.getNestedType(getGenericType(), 0);
                if (type == null) {
                    type = Object.class;
                }
            }
            actualType = type;
        }
        return actualType;
    }

    public final Object getTypeDescriptor() {
        return typeDescriptor;
    }

    public final void setTypeDescriptor(Object typeDescriptor) {
        this.typeDescriptor = typeDescriptor;
    }

    public final Object bind(HttpRequest request, HttpResponse response) {
        return getToolKit().bind(this, request, response);
    }

    public String getDescription() {
        return name;
    }

    public abstract Class<?> getType();

    public abstract Type getGenericType();

    @Override
    public String toString() {
        return "ParameterMeta{name='" + name + "', type=" + getType() + '}';
    }
}
