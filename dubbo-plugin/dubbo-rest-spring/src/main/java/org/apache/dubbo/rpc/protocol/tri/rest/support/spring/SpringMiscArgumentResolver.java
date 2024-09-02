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
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.ArgumentResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;

import javax.servlet.http.HttpServletRequest;

import java.util.HashSet;
import java.util.Set;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

@Activate(onClass = "org.springframework.web.context.request.WebRequest")
public class SpringMiscArgumentResolver implements ArgumentResolver {

    private static final Set<Class<?>> SUPPORTED_TYPES = new HashSet<>();

    static {
        SUPPORTED_TYPES.add(WebRequest.class);
        SUPPORTED_TYPES.add(NativeWebRequest.class);
        SUPPORTED_TYPES.add(HttpEntity.class);
        SUPPORTED_TYPES.add(HttpHeaders.class);
    }

    @Override
    public boolean accept(ParameterMeta parameter) {
        return SUPPORTED_TYPES.contains(parameter.getActualType());
    }

    @Override
    public Object resolve(ParameterMeta parameter, HttpRequest request, HttpResponse response) {
        Class<?> type = parameter.getActualType();
        if (type == WebRequest.class || type == NativeWebRequest.class) {
            return new ServletWebRequest((HttpServletRequest) request);
        }
        if (type == HttpEntity.class) {
            return new HttpEntity<>(
                    CollectionUtils.toMultiValueMap(request.headers().asMap()));
        }
        if (type == HttpHeaders.class) {
            return new HttpHeaders(
                    CollectionUtils.toMultiValueMap(request.headers().asMap()));
        }
        return null;
    }
}
