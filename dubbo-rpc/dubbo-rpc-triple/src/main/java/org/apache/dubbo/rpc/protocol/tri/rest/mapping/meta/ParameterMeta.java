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

import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.rpc.protocol.tri.rest.Messages;
import org.apache.dubbo.rpc.protocol.tri.rest.RestException;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RestToolKit;
import org.apache.dubbo.rpc.protocol.tri.rest.util.TypeUtils;

import javax.annotation.Nullable;

import java.lang.reflect.Type;
import java.util.Collection;

public abstract class ParameterMeta extends AnnotationSupport {

    private final String prefix;
    private final String name;
    private Boolean simple;
    private Class<?> actualType;
    private Type actualGenericType;
    private BeanMeta beanMeta;
    private NamedValueMeta namedValueMeta;

    protected ParameterMeta(RestToolKit toolKit, String prefix, String name) {
        super(toolKit);
        this.prefix = prefix;
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

    @Nullable
    public String getName() {
        return name;
    }

    public String getRequiredName() {
        String name = getName();
        if (name == null) {
            throw new RestException(Messages.ARGUMENT_NAME_MISSING, getType());
        }
        return name;
    }

    public final boolean isSimple() {
        Boolean simple = this.simple;
        if (simple == null) {
            Class<?> type = Collection.class.isAssignableFrom(getType())
                    ? TypeUtils.getNestedActualType(getGenericType(), 0)
                    : getActualType();
            simple = TypeUtils.isSimpleProperty(type);
            this.simple = simple;
        }
        return simple;
    }

    public final Class<?> getActualType() {
        Class<?> type = actualType;
        if (type == null) {
            type = getType();
            if (TypeUtils.isWrapperType(type)) {
                type = TypeUtils.getNestedActualType(getGenericType(), 0);
                if (type == null) {
                    type = Object.class;
                }
            }
            actualType = type;
        }
        return type;
    }

    public final Type getActualGenericType() {
        Type type = actualGenericType;
        if (type == null) {
            type = getGenericType();
            if (TypeUtils.isWrapperType(TypeUtils.getActualType(type))) {
                type = TypeUtils.getNestedGenericType(type, 0);
                if (type == null) {
                    type = Object.class;
                }
            }
            actualGenericType = type;
        }
        return type;
    }

    public final BeanMeta getBeanMeta() {
        BeanMeta beanMeta = this.beanMeta;
        if (beanMeta == null) {
            this.beanMeta = beanMeta = new BeanMeta(getToolKit(), getActualType());
        }
        return beanMeta;
    }

    public final Object bind(HttpRequest request, HttpResponse response) {
        return getToolKit().bind(this, request, response);
    }

    public final NamedValueMeta getNamedValueMeta() {
        NamedValueMeta namedValueMeta = this.namedValueMeta;
        if (namedValueMeta == null) {
            namedValueMeta = getToolKit().getNamedValueMeta(this);
            if (namedValueMeta == null) {
                namedValueMeta = NamedValueMeta.EMPTY;
            }
            this.namedValueMeta = namedValueMeta;
        }
        return namedValueMeta;
    }

    public int getIndex() {
        return -1;
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
