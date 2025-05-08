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

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.List;

public final class MethodParameterMeta extends ParameterMeta {

    private final List<Parameter> hierarchy;
    private final Parameter parameter;
    private final int index;
    private final MethodMeta methodMeta;

    public MethodParameterMeta(List<Parameter> hierarchy, String name, int index, MethodMeta methodMeta) {
        super(methodMeta.getToolKit(), name);
        this.hierarchy = hierarchy;
        parameter = hierarchy.get(0);
        this.index = index;
        this.methodMeta = methodMeta;
    }

    public List<Parameter> getHierarchy() {
        return hierarchy;
    }

    public Parameter getParameter() {
        return parameter;
    }

    @Override
    public int getIndex() {
        return index;
    }

    public MethodMeta getMethodMeta() {
        return methodMeta;
    }

    @Override
    public Class<?> getType() {
        return parameter.getType();
    }

    @Override
    public Type getGenericType() {
        return parameter.getParameterizedType();
    }

    @Override
    public String getDescription() {
        return "parameter [" + getMethod() + "] in {" + index + "}";
    }

    public Method getMethod() {
        return methodMeta.getMethod();
    }

    @Override
    protected AnnotatedElement getAnnotatedElement() {
        return parameter;
    }

    @Override
    public List<? extends AnnotatedElement> getAnnotatedElements() {
        return hierarchy;
    }

    @Override
    public int hashCode() {
        return 31 * getMethod().hashCode() + index;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != MethodParameterMeta.class) {
            return false;
        }
        MethodParameterMeta other = (MethodParameterMeta) obj;
        return getMethod().equals(other.getMethod()) && index == other.index;
    }

    @Override
    public String toString() {
        return "MethodParameterMeta{parameter=" + parameter + '}';
    }
}
