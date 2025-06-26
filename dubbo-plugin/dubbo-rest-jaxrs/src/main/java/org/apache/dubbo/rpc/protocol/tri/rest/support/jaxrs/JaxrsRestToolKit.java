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

import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.util.AbstractRestToolKit;

import javax.ws.rs.ext.ParamConverter;

final class JaxrsRestToolKit extends AbstractRestToolKit {

    private final BeanArgumentBinder binder;
    private final ParamConverterFactory paramConverterFactory;

    public JaxrsRestToolKit(FrameworkModel frameworkModel) {
        super(frameworkModel);
        binder = new BeanArgumentBinder(argumentResolver);
        paramConverterFactory = new ParamConverterFactory();
    }

    @Override
    public int getDialect() {
        return RestConstants.DIALECT_JAXRS;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object convert(Object value, ParameterMeta parameter) {
        ParamConverter converter = paramConverterFactory.getParamConverter(
                parameter.getType(), parameter.getGenericType(), parameter.getRawAnnotations());
        if (converter != null) {
            return value instanceof String ? converter.fromString((String) value) : converter.toString(value);
        }
        return super.convert(value, parameter);
    }

    @Override
    public Object bind(ParameterMeta parameter, HttpRequest request, HttpResponse response) {
        return binder.bind(parameter, request, response);
    }
}
