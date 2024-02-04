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
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpRequest.FileUpload;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.AnnotationMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.NamedValueMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Activate(onClass = "org.springframework.web.bind.annotation.RequestPart")
public class RequestPartArgumentResolver extends AbstractSpringArgumentResolver {

    @Override
    public Class<Annotation> accept() {
        return Annotations.RequestPart.type();
    }

    @Override
    protected NamedValueMeta createNamedValueMeta(ParameterMeta param, AnnotationMeta<Annotation> ann) {
        return new NamedValueMeta(ann.getValue(), Helper.isRequired(ann), null);
    }

    @Override
    protected Object resolveValue(NamedValueMeta meta, HttpRequest request, HttpResponse response) {
        return request.part(meta.name());
    }

    @Override
    protected Object resolveCollectionValue(NamedValueMeta meta, HttpRequest request, HttpResponse response) {
        return request.parts();
    }

    @Override
    protected Object resolveMapValue(NamedValueMeta meta, HttpRequest request, HttpResponse response) {
        Collection<FileUpload> parts = request.parts();
        if (parts.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, FileUpload> result = new LinkedHashMap<>(parts.size());
        for (FileUpload part : parts) {
            result.put(part.name(), part);
        }
        return result;
    }
}
