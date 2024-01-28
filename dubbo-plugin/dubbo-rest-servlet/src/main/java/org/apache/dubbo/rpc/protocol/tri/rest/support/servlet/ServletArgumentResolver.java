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
package org.apache.dubbo.rpc.protocol.tri.rest.support.servlet;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.rpc.protocol.tri.rest.RestException;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.ArgumentResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

@Activate(onClass = "javax.servlet.http.HttpServletRequest")
public class ServletArgumentResolver implements ArgumentResolver {

    private static final Set<Class<?>> SUPPORTED_TYPES = new HashSet<>();

    static {
        SUPPORTED_TYPES.add(ServletRequest.class);
        SUPPORTED_TYPES.add(HttpServletRequest.class);
        SUPPORTED_TYPES.add(ServletResponse.class);
        SUPPORTED_TYPES.add(HttpServletResponse.class);
        SUPPORTED_TYPES.add(HttpSession.class);
        SUPPORTED_TYPES.add(Cookie.class);
        SUPPORTED_TYPES.add(Cookie[].class);
        SUPPORTED_TYPES.add(Reader.class);
        SUPPORTED_TYPES.add(Writer.class);
    }

    @Override
    public boolean accept(ParameterMeta parameter) {
        return SUPPORTED_TYPES.contains(parameter.getActualType());
    }

    @Override
    public Object resolve(ParameterMeta parameter, HttpRequest request, HttpResponse response) {
        Class<?> type = parameter.getActualType();
        if (type == ServletRequest.class || type == HttpServletRequest.class) {
            return request;
        }
        if (type == ServletResponse.class || type == HttpServletResponse.class) {
            return response;
        }
        if (type == HttpSession.class) {
            return ((HttpServletRequest) request).getSession();
        }
        if (type == Cookie.class) {
            return Helper.convert(request.cookie(parameter.getRequiredName()));
        }
        if (type == Cookie[].class) {
            return ((HttpServletRequest) request).getCookies();
        }
        if (type == Reader.class) {
            try {
                return ((HttpServletRequest) request).getReader();
            } catch (IOException e) {
                throw new RestException(e);
            }
        }
        if (type == Writer.class) {
            try {
                return ((HttpServletResponse) response).getWriter();
            } catch (IOException e) {
                throw new RestException(e);
            }
        }
        return null;
    }
}
