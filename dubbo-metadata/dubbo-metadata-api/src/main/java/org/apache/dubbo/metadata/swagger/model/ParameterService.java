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

import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class ParameterService {

    public static Parameter mergeParameter(List<Parameter> operationParameters, Parameter parameter) {
        Parameter result = parameter;
        if (parameter != null && parameter.getName() != null) {
            final String name = parameter.getName();
            final String in = parameter.getIn();
            Parameter paramDoc = operationParameters.stream()
                    .filter(p -> name.equals(p.getName())
                            && (StringUtils.isEmpty(in) || StringUtils.isEmpty(p.getIn()) || in.equals(p.getIn())))
                    .findAny()
                    .orElse(null);
            if (paramDoc != null) {
                mergeParameter(parameter, paramDoc);
                result = paramDoc;
            } else operationParameters.add(result);
        }
        return result;
    }

    /**
     * Merge parameter.
     *
     * @param paramCalcul the param calcul
     * @param paramDoc the param doc
     */
    public static void mergeParameter(Parameter paramCalcul, Parameter paramDoc) {
        if (StringUtils.isBlank(paramDoc.getDescription())) paramDoc.setDescription(paramCalcul.getDescription());

        if (StringUtils.isBlank(paramDoc.getIn())) paramDoc.setIn(paramCalcul.getIn());

        if (paramDoc.getExample() == null) paramDoc.setExample(paramCalcul.getExample());

        if (paramDoc.getDeprecated() == null) paramDoc.setDeprecated(paramCalcul.getDeprecated());

        if (paramDoc.getRequired() == null) paramDoc.setRequired(paramCalcul.getRequired());

        if (paramDoc.getAllowEmptyValue() == null) paramDoc.setAllowEmptyValue(paramCalcul.getAllowEmptyValue());

        if (paramDoc.getAllowReserved() == null) paramDoc.setAllowReserved(paramCalcul.getAllowReserved());

        if (StringUtils.isBlank(paramDoc.get$ref())) paramDoc.set$ref(paramDoc.get$ref());

        if (paramDoc.getSchema() == null && paramDoc.getContent() == null) paramDoc.setSchema(paramCalcul.getSchema());

        if (paramDoc.getExtensions() == null) paramDoc.setExtensions(paramCalcul.getExtensions());

        if (paramDoc.getStyle() == null) paramDoc.setStyle(paramCalcul.getStyle());

        if (paramDoc.getExplode() == null) paramDoc.setExplode(paramCalcul.getExplode());
    }
}
