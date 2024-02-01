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
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.ArgumentResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RequestUtils;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.resteasy.specimpl.ResteasyHttpHeaders;
import org.jboss.resteasy.spi.ResteasyUriInfo;

@Activate(onClass = "javax.ws.rs.Path")
public class JaxrsMiscArgumentResolver implements ArgumentResolver {

    private static final Set<Class<?>> SUPPORTED_TYPES = new HashSet<>();

    static {
        SUPPORTED_TYPES.add(Cookie.class);
        SUPPORTED_TYPES.add(Form.class);
        SUPPORTED_TYPES.add(HttpHeaders.class);
        SUPPORTED_TYPES.add(MediaType.class);
        SUPPORTED_TYPES.add(MultivaluedMap.class);
        SUPPORTED_TYPES.add(UriInfo.class);
    }

    @Override
    public boolean accept(ParameterMeta parameter) {
        return SUPPORTED_TYPES.contains(parameter.getActualType());
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Object resolve(ParameterMeta parameter, HttpRequest request, HttpResponse response) {
        Class<?> type = parameter.getActualType();
        if (Cookie.class.isAssignableFrom(type)) {
            return Helper.convert(request.cookie(parameter.getRequiredName()));
        }
        if (Form.class.isAssignableFrom(type)) {
            return RequestUtils.getFormParametersMap(request);
        }
        if (HttpHeaders.class.isAssignableFrom(type)) {
            return new ResteasyHttpHeaders(new MultivaluedMapWrapper<>(request.headers()));
        }
        if (MediaType.class.isAssignableFrom(type)) {
            return Helper.toMediaType(request.mediaType());
        }
        if (MultivaluedMap.class.isAssignableFrom(type)) {
            return new MultivaluedMapWrapper<>((Map) RequestUtils.getParametersMap(request));
        }
        if (UriInfo.class.isAssignableFrom(type)) {
            return new ResteasyUriInfo(request.rawPath(), request.query(), "/");
        }
        return null;
    }
}
