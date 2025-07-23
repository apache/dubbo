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
package org.apache.dubbo.rpc.protocol.tri.rest.support.jaxrs;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.remoting.http12.rest.ParamType;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.NamedValueMeta;

import java.lang.annotation.Annotation;

/**
 * TODO: support nested values: (e.g., 'telephoneNumbers[0].countryCode' 'address[INVOICE].street')
 */
@Activate(onClass = "javax.ws.rs.FormParam")
public class FormParamArgumentResolver extends AbstractJaxrsArgumentResolver {

    @Override
    public Class<Annotation> accept() {
        return Annotations.FormParam.type();
    }

    @Override
    protected ParamType getParamType(NamedValueMeta meta) {
        return ParamType.Form;
    }

    @Override
    protected Object resolveValue(NamedValueMeta meta, HttpRequest request, HttpResponse response) {
        return CollectionUtils.first(request.formParameterValues(getFullName(meta)));
    }

    @Override
    protected Object resolveCollectionValue(NamedValueMeta meta, HttpRequest request, HttpResponse response) {
        return request.formParameterValues(getFullName(meta));
    }

    private String getFullName(NamedValueMeta meta) {
        String prefix = meta.parameter().getPrefix();
        return prefix == null ? meta.name() : prefix + '.' + meta.name();
    }
}
