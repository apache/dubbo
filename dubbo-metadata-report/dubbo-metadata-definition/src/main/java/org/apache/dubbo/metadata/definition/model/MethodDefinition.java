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
package org.apache.dubbo.metadata.definition.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 2015/1/27.
 */
public class MethodDefinition {

    private String name;
    private String[] parameterTypes;
    private String returnType;
    private List<TypeDefinition> parameters;

    public String getName() {
        return name;
    }

    public List<TypeDefinition> getParameters() {
        if (parameters == null) {
            parameters = new ArrayList<TypeDefinition>();
        }
        return parameters;
    }

    public String[] getParameterTypes() {
        return parameterTypes;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setParameters(List<TypeDefinition> parameters) {
        this.parameters = parameters;
    }

    public void setParameterTypes(String[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    @Override
    public String toString() {
        return "MethodDefinition [name=" + name + ", parameterTypes=" + Arrays.toString(parameterTypes)
                + ", returnType=" + returnType + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MethodDefinition)) return false;
        MethodDefinition that = (MethodDefinition) o;
        return Objects.equals(getName(), that.getName()) &&
                Arrays.equals(getParameterTypes(), that.getParameterTypes()) &&
                Objects.equals(getReturnType(), that.getReturnType()) &&
                Objects.equals(getParameters(), that.getParameters());
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(getName(), getReturnType(), getParameters());
        result = 31 * result + Arrays.hashCode(getParameterTypes());
        return result;
    }
}
