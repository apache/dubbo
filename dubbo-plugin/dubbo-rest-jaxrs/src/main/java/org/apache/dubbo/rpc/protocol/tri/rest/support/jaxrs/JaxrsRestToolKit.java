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
import org.apache.dubbo.rpc.protocol.tri.rest.util.DefaultRestToolKit;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

final class JaxrsRestToolKit extends DefaultRestToolKit {

    private final BeanArgumentBinder binder;

    private final JaxrsParamConverter jaxrsParamConverter;

    public JaxrsRestToolKit(FrameworkModel frameworkModel) {
        super(frameworkModel);
        binder = new BeanArgumentBinder(frameworkModel);
        jaxrsParamConverter = new JaxrsParamConverter(frameworkModel);
    }

    @Override
    public Object convert(Object value, ParameterMeta parameter) {
        if (MultivaluedMap.class.isAssignableFrom(parameter.getType())) {
            if (value instanceof MultivaluedMap) {
                return value;
            }
            return typeConverter.convert(value, MultivaluedHashMap.class);
        }
        if (jaxrsParamConverter.canConvert(value.getClass(), parameter.getActualType())) {
            Object convertResult = jaxrsParamConverter.convert(value.getClass(), parameter.getType(), value);
            if (convertResult != null) {
                return convertResult;
            }
        }
        return super.convert(value, parameter);
    }

    @Override
    public int getDialect() {
        return RestConstants.DIALECT_JAXRS;
    }

    @Override
    public Object bind(ParameterMeta parameter, HttpRequest request, HttpResponse response) {
        return binder.bind(parameter, request, response);
    }
}
