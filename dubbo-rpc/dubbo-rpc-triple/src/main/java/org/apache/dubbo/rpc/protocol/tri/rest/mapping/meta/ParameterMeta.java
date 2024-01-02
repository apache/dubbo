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

public final class ParameterMeta extends AnnotationSupport {

    private final List<Parameter> hierarchy;
    private final Parameter parameter;
    private final String name;
    private final int index;
    private final MethodMeta methodMeta;
    private Object typeDescriptor;

    public ParameterMeta(List<Parameter> hierarchy, String name, int index, MethodMeta methodMeta) {
        super(methodMeta.getToolKit());
        this.hierarchy = hierarchy;
        parameter = hierarchy.get(0);
        this.name = name;
        this.index = index;
        this.methodMeta = methodMeta;
    }

    public List<Parameter> getHierarchy() {
        return hierarchy;
    }

    public Parameter getParameter() {
        return parameter;
    }

    public String getName() {
        return name;
    }

    public int getIndex() {
        return index;
    }

    public MethodMeta getMethodMeta() {
        return methodMeta;
    }

    public Object getTypeDescriptor() {
        return typeDescriptor;
    }

    public void setTypeDescriptor(Object typeDescriptor) {
        this.typeDescriptor = typeDescriptor;
    }

    public Class<?> getType() {
        return parameter.getType();
    }

    public Type getGenericType() {
        return parameter.getParameterizedType();
    }

    public Method getMethod() {
        return methodMeta.getMethod();
    }

    @Override
    protected List<? extends AnnotatedElement> getAnnotatedElements() {
        return hierarchy;
    }

    @Override
    protected AnnotatedElement getAnnotatedElement() {
        return parameter;
    }

    @Override
    public String toString() {
        return "ParameterMeta{parameter=" + parameter + '}';
    }
}
