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

import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.AnnotationBaseArgumentResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.AnnotationMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;

import java.lang.annotation.Annotation;
import java.util.Map;

public class PathVariableArgumentResolver implements AnnotationBaseArgumentResolver<Annotation> {

    @Override
    public Class<Annotation> accept() {
        return Annotations.PathVariable.type();
    }

    @Override
    public Object resolve(
            ParameterMeta parameter,
            AnnotationMeta<Annotation> annotation,
            HttpRequest request,
            HttpResponse response) {
        Map<String, String> variableMap = request.attribute(RestConstants.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (variableMap != null) {
            String value = variableMap.get(annotation.getValue());
            if (value != null) {
                return value;
            }
        }
        if (annotation.getBoolean("required")) {
            throw new IllegalArgumentException("PathVariable '" + annotation.getValue() + "' not found");
        }
        return null;
    }
}
