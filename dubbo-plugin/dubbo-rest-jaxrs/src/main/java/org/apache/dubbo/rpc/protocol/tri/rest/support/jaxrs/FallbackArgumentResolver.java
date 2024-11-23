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
import org.apache.dubbo.common.io.StreamUtils;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.remoting.http12.rest.ParamType;
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;
import org.apache.dubbo.rpc.protocol.tri.rest.RestException;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.AbstractArgumentResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.NamedValueMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RequestUtils;
import org.apache.dubbo.rpc.protocol.tri.rest.util.TypeUtils;

import java.io.IOException;

@Activate(order = Integer.MAX_VALUE - 10000, onClass = "javax.ws.rs.Path")
public class FallbackArgumentResolver extends AbstractArgumentResolver {

    @Override
    public boolean accept(ParameterMeta param) {
        return param.getToolKit().getDialect() == RestConstants.DIALECT_JAXRS;
    }

    @Override
    protected NamedValueMeta createNamedValueMeta(ParameterMeta param) {
        return new NamedValueMeta(null, param.isAnnotated(Annotations.Nonnull), Helper.defaultValue(param))
                .setParamType(param.isSimple() ? null : ParamType.Body);
    }

    @Override
    protected Object resolveValue(NamedValueMeta meta, HttpRequest request, HttpResponse response) {
        Object value = RequestUtils.decodeBody(request, meta.genericType());
        if (value != null) {
            return value;
        }
        if (meta.parameter().isSimple()) {
            return request.parameter(meta.name());
        }
        return null;
    }

    @Override
    protected Object resolveCollectionValue(NamedValueMeta meta, HttpRequest request, HttpResponse response) {
        Class<?> type = meta.type();
        if (type == byte[].class) {
            try {
                return StreamUtils.readBytes(request.inputStream());
            } catch (IOException e) {
                throw new RestException(e);
            }
        }
        Object value = RequestUtils.decodeBody(request, meta.genericType());
        if (value != null) {
            return value;
        }
        if (TypeUtils.isSimpleProperty(meta.nestedType(0))) {
            return request.parameterValues(meta.name());
        }
        return null;
    }

    @Override
    protected Object resolveMapValue(NamedValueMeta meta, HttpRequest request, HttpResponse response) {
        Object value = RequestUtils.decodeBody(request, meta.genericType());
        if (value != null) {
            return value;
        }
        if (TypeUtils.isSimpleProperty(meta.nestedType(1))) {
            return RequestUtils.getParametersMap(request);
        }
        return null;
    }
}
