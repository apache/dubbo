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
package org.apache.dubbo.metadata.rest;


import java.lang.reflect.Parameter;

/**
 *  description of service method args info
 */
public class ArgInfo {
    /**
     * method arg index 0,1,2,3
     */
    private int index;
    /**
     * method annotation name or name
     */
    private String annotationNameAttribute;

    /**
     * param annotation type
     */
    private Class paramAnnotationType;

    /**
     * param Type
     */
    private Class paramType;

    /**
     * param name
     */
    private String paramName;

    /**
     * url split("/") String[n]  index
     */
    private int urlSplitIndex;

    private Object defaultValue;

    private boolean formContentType;

    public ArgInfo(int index, String name, Class paramType) {
        this.index = index;
        this.paramName = name;
        this.paramType = paramType;
    }

    public ArgInfo(int index, Parameter parameter) {
        this(index, parameter.getName(), parameter.getType());
    }

    public ArgInfo() {
    }

    public int getIndex() {
        return index;
    }

    public ArgInfo setIndex(int index) {
        this.index = index;
        return this;
    }

    public String getAnnotationNameAttribute() {
        if (annotationNameAttribute == null) {
            // such as String param no annotation
            return paramName;
        }
        return annotationNameAttribute;
    }

    public ArgInfo setAnnotationNameAttribute(String annotationNameAttribute) {
        this.annotationNameAttribute = annotationNameAttribute;
        return this;
    }

    public Class getParamAnnotationType() {
        return paramAnnotationType;
    }

    public ArgInfo setParamAnnotationType(Class paramAnnotationType) {
        this.paramAnnotationType = paramAnnotationType;
        return this;
    }

    public Class getParamType() {
        return paramType;
    }

    public void setParamType(Class paramType) {
        this.paramType = paramType;
    }


    public int getUrlSplitIndex() {
        return urlSplitIndex;
    }

    public void setUrlSplitIndex(int urlSplitIndex) {
        this.urlSplitIndex = urlSplitIndex;
    }

    public static ArgInfo build() {
        return new ArgInfo();
    }

    public static ArgInfo build(int index, Parameter parameter) {
        return new ArgInfo(index, parameter);
    }

    public String getParamName() {
        return paramName;
    }

    public ArgInfo setParamName(String paramName) {
        this.paramName = paramName;
        return this;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public ArgInfo setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public boolean isFormContentType() {
        return formContentType;
    }

    public ArgInfo setFormContentType(boolean isFormContentType) {
        this.formContentType = isFormContentType;
        return this;
    }

    @Override
    public String toString() {
        return "ArgInfo{" +
            "index=" + index +
            ", annotationNameAttribute='" + annotationNameAttribute + '\'' +
            ", paramAnnotationType=" + paramAnnotationType +
            ", paramType=" + paramType +
            ", paramName='" + paramName + '\'' +
            ", urlSplitIndex=" + urlSplitIndex +
            ", defaultValue=" + defaultValue +
            ", formContentType=" + formContentType +
            '}';
    }
}
