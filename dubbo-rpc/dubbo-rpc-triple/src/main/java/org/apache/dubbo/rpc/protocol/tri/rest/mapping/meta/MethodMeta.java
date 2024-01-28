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
import java.util.ArrayList;
import java.util.List;

public final class MethodMeta extends AnnotationSupport {

    private final List<Method> hierarchy;
    private final Method method;
    private final ParameterMeta[] parameters;
    private final ServiceMeta serviceMeta;

    public MethodMeta(List<Method> hierarchy, ServiceMeta serviceMeta) {
        super(serviceMeta.getToolKit());
        this.hierarchy = hierarchy;
        method = hierarchy.get(0);
        parameters = initParameters(method, hierarchy);
        this.serviceMeta = serviceMeta;
    }

    private ParameterMeta[] initParameters(Method method, List<Method> hierarchy) {
        int count = method.getParameterCount();
        List<List<Parameter>> parameterHierarchies = new ArrayList<>(count);
        for (int i = 0, len = hierarchy.size(); i < len; i++) {
            Method m = hierarchy.get(i);
            Parameter[] mps = m.getParameters();
            for (int j = 0; j < count; j++) {
                List<Parameter> parameterHierarchy;
                if (parameterHierarchies.size() <= j) {
                    parameterHierarchy = new ArrayList<>(len);
                    parameterHierarchies.add(parameterHierarchy);
                } else {
                    parameterHierarchy = parameterHierarchies.get(j);
                }
                parameterHierarchy.add(mps[j]);
            }
        }

        String[] parameterNames = getToolKit().getParameterNames(method);
        ParameterMeta[] parameters = new ParameterMeta[count];
        for (int i = 0; i < count; i++) {
            String parameterName = parameterNames == null ? null : parameterNames[i];
            parameters[i] = new MethodParameterMeta(parameterHierarchies.get(i), parameterName, i, this);
        }
        return parameters;
    }

    public List<Method> getHierarchy() {
        return hierarchy;
    }

    public Method getMethod() {
        return method;
    }

    public ParameterMeta[] getParameters() {
        return parameters;
    }

    public ServiceMeta getServiceMeta() {
        return serviceMeta;
    }

    public Class<?> getReturnType() {
        return method.getReturnType();
    }

    public Type getGenericReturnType() {
        return method.getGenericReturnType();
    }

    @Override
    protected List<? extends AnnotatedElement> getAnnotatedElements() {
        return hierarchy;
    }

    @Override
    protected AnnotatedElement getAnnotatedElement() {
        return method;
    }

    @Override
    public int hashCode() {
        return method.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != MethodMeta.class) {
            return false;
        }
        return method.equals(((MethodMeta) obj).method);
    }

    @Override
    public String toString() {
        return "MethodMeta{method=" + method + '}';
    }
}
