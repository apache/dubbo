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

import org.apache.dubbo.metadata.definition.util.ClassUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 2015/1/27.
 */
public class ServiceDefinition implements Serializable {

    /**
     * the canonical name of interface
     *
     * @see Class#getCanonicalName()
     */
    private String canonicalName;

    /**
     * the location of class file
     *
     * @see ClassUtils#getCodeSource(Class)
     */
    private String codeSource;

    private List<MethodDefinition> methods;

    /**
     * the definitions of type
     */
    private List<TypeDefinition> types;

    /**
     * the definitions of annotations
     */
    private List<String> annotations;

    public String getCanonicalName() {
        return canonicalName;
    }

    public String getCodeSource() {
        return codeSource;
    }

    public List<MethodDefinition> getMethods() {
        if (methods == null) {
            methods = new ArrayList<>();
        }
        return methods;
    }

    public List<TypeDefinition> getTypes() {
        if (types == null) {
            types = new ArrayList<>();
        }
        return types;
    }

    public String getUniqueId() {
        return canonicalName + "@" + codeSource;
    }

    public void setCanonicalName(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public void setCodeSource(String codeSource) {
        this.codeSource = codeSource;
    }

    public void setMethods(List<MethodDefinition> methods) {
        this.methods = methods;
    }

    public void setTypes(List<TypeDefinition> types) {
        this.types = types;
    }

    public List<String> getAnnotations() {
        if (annotations == null) {
            annotations = Collections.emptyList();
        }
        return annotations;
    }

    public void setAnnotations(List<String> annotations) {
        this.annotations = annotations;
    }

    @Override
    public String toString() {
        return "ServiceDefinition [canonicalName=" + canonicalName + ", codeSource=" + codeSource + ", methods="
                + methods + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServiceDefinition)) {
            return false;
        }
        ServiceDefinition that = (ServiceDefinition) o;
        return Objects.equals(getCanonicalName(), that.getCanonicalName()) &&
                Objects.equals(getCodeSource(), that.getCodeSource()) &&
                Objects.equals(getMethods(), that.getMethods()) &&
                Objects.equals(getTypes(), that.getTypes());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCanonicalName(), getCodeSource(), getMethods(), getTypes());
    }
}
