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
package org.apache.dubbo.rpc.protocol.tri.rest.support.spring;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.remoting.http12.rest.ParamType;
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.AnnotationMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.NamedValueMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RequestUtils;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Activate(onClass = "org.springframework.web.bind.annotation.MatrixVariable")
public class MatrixVariableArgumentResolver extends AbstractSpringArgumentResolver {

    @Override
    public Class<Annotation> accept() {
        return Annotations.MatrixVariable.type();
    }

    @Override
    protected ParamType getParamType(NamedValueMeta meta) {
        return ParamType.MatrixVariable;
    }

    @Override
    protected NamedValueMeta createNamedValueMeta(ParameterMeta param, AnnotationMeta<Annotation> anno) {
        return new MatrixNamedValueMeta(
                anno.getValue(),
                Helper.isRequired(anno),
                Helper.defaultValue(anno),
                Helper.defaultValue(anno, "pathVar"));
    }

    @Override
    protected Object resolveValue(NamedValueMeta meta, HttpRequest request, HttpResponse response) {
        return CollectionUtils.first(doResolveCollectionValue(meta, request));
    }

    @Override
    protected Object resolveCollectionValue(NamedValueMeta meta, HttpRequest request, HttpResponse response) {
        return doResolveCollectionValue(meta, request);
    }

    private static List<String> doResolveCollectionValue(NamedValueMeta meta, HttpRequest request) {
        String name = meta.name();
        Map<String, String> variableMap = request.attribute(RestConstants.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (variableMap == null) {
            return Collections.emptyList();
        }
        List<String> result = null;
        String pathVar = ((MatrixNamedValueMeta) meta).pathVar;
        if (pathVar == null) {
            result = RequestUtils.parseMatrixVariableValues(variableMap, name);
        } else {
            String value = variableMap.get(pathVar);
            if (value != null) {
                Map<String, List<String>> matrixVariables = RequestUtils.parseMatrixVariables(value);
                if (matrixVariables != null) {
                    List<String> values = matrixVariables.get(name);
                    if (values != null) {
                        return values;
                    }
                }
            }
        }
        return result == null ? Collections.emptyList() : result;
    }

    @Override
    protected Object resolveMapValue(NamedValueMeta meta, HttpRequest request, HttpResponse response) {
        Map<String, String> variableMap = request.attribute(RestConstants.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (variableMap == null) {
            return Collections.emptyMap();
        }
        Map<String, List<String>> result = null;
        String pathVar = ((MatrixNamedValueMeta) meta).pathVar;
        if (pathVar == null) {
            for (Map.Entry<String, String> entry : variableMap.entrySet()) {
                Map<String, List<String>> matrixVariables = RequestUtils.parseMatrixVariables(entry.getValue());
                if (matrixVariables == null) {
                    continue;
                }
                if (result == null) {
                    result = new HashMap<>();
                }
                for (Map.Entry<String, List<String>> matrixEntry : matrixVariables.entrySet()) {
                    result.computeIfAbsent(matrixEntry.getKey(), k -> new ArrayList<>())
                            .addAll(matrixEntry.getValue());
                }
            }
        } else {
            String value = variableMap.get(pathVar);
            if (value != null) {
                result = RequestUtils.parseMatrixVariables(value);
            }
        }
        return result == null ? Collections.emptyMap() : result;
    }

    private static class MatrixNamedValueMeta extends NamedValueMeta {

        private final String pathVar;

        MatrixNamedValueMeta(String name, boolean required, String defaultValue, String pathVar) {
            super(name, required, defaultValue);
            this.pathVar = pathVar;
        }
    }
}
