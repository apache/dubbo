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
package org.apache.dubbo.rpc.proxy.asm;

import java.lang.reflect.Type;
import java.util.List;

public class MethodStatement {

    private static final Class[] NOT_PRAMETER_CLASS = new Class[0];

    private String method;

    private String alias;

    private Type returnType;

    private Type[] returnGeneric;

    // 如果有泛型怎么办?
    private List<ParameterSteaement> parameterTypes;

    private Class[] parameterClass;

    boolean futureReturnType;

    private Type[] abnormalTypes;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public Type getReturnType() {
        return returnType;
    }

    public void setReturnType(Type returnType) {
        this.returnType = returnType;
    }

    public Type[] getReturnGeneric() {
        return returnGeneric;
    }

    public void setReturnGeneric(Type[] returnGeneric) {
        this.returnGeneric = returnGeneric;
    }

    public List<ParameterSteaement> getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(List<ParameterSteaement> parameterTypes) {
        this.parameterTypes = parameterTypes;
        if (parameterTypes != null) {
            parameterClass = new Class[parameterTypes.size()];
            int i = 0;
            for (ParameterSteaement ps : parameterTypes) {
                parameterClass[i++] = ps.getType();
            }
        }

    }

    public Class[] getParameterClass() {
        return parameterClass;
    }

    public boolean isFutureReturnType() {
        return futureReturnType;
    }

    public void setFutureReturnType(boolean futureReturnType) {
        this.futureReturnType = futureReturnType;
    }

    public Type[] getAbnormalTypes() {
        return abnormalTypes;
    }

    public void setAbnormalTypes(Type[] abnormalTypes) {
        this.abnormalTypes = abnormalTypes;
    }

    static class ParameterSteaement {

        private String parameterName;

        private Class<?> clazz;

        private Type[] genericTypes;

        public String getParameterName() {
            return parameterName;
        }

        public void setParameterName(String parameterName) {
            this.parameterName = parameterName;
        }

        public Class<?> getType() {
            return clazz;
        }

        public void setType(Class<?> clazz) {
            this.clazz = clazz;
        }

        public Type[] getGenericTypes() {
            return genericTypes;
        }

        public void setGenericTypes(Type[] genericTypes) {
            this.genericTypes = genericTypes;
        }

    }
}
