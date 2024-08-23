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
package org.apache.dubbo.metadata.swagger.model;

import org.apache.dubbo.metadata.swagger.model.parameters.Parameter;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;

public class ParameterInfo {

    /**
     * The Method parameter.
     */
    private final ParameterMeta methodParameter;

    /**
     * The P name.
     */
    private String pName;

    /**
     * The Parameter id.
     */
    private ParameterId parameterId;

    /**
     * The Required.
     */
    private boolean required;

    /**
     * The Param type.
     */
    private String paramType;

    /**
     * The Default value.
     */
    private Object defaultValue;

    /**
     * The Parameter model.
     */
    private Parameter parameterModel;

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getParamType() {
        return paramType;
    }

    public void setParamType(String paramType) {
        this.paramType = paramType;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public ParameterId getParameterId() {
        return parameterId;
    }

    public void setParameterId(ParameterId parameterId) {
        this.parameterId = parameterId;
    }

    public String getpName() {
        return pName;
    }

    public void setpName(String pName) {
        this.pName = pName;
    }

    public ParameterMeta getMethodParameter() {
        return methodParameter;
    }

    public Parameter getParameterModel() {
        return parameterModel;
    }

    public void setParameterModel(Parameter parameterModel) {
        this.parameterModel = parameterModel;
    }

    public ParameterInfo(String pName, ParameterMeta methodParameter, ParameterService parameterService) {
        this.methodParameter = methodParameter;
        this.pName = pName;
        this.required = false;
        this.parameterId = new ParameterId(this.pName, paramType);
    }
}
