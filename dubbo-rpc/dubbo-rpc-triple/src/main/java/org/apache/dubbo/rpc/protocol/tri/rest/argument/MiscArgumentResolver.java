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
package org.apache.dubbo.rpc.protocol.tri.rest.argument;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Activate
public class MiscArgumentResolver implements ArgumentResolver {

    private static final Set<Class<?>> SUPPORTED_TYPES = new HashSet<>();

    static {
        SUPPORTED_TYPES.add(HttpRequest.class);
        SUPPORTED_TYPES.add(HttpResponse.class);
        SUPPORTED_TYPES.add(HttpMethods.class);
        SUPPORTED_TYPES.add(Locale.class);
        SUPPORTED_TYPES.add(InputStream.class);
        SUPPORTED_TYPES.add(OutputStream.class);
    }

    @Override
    public boolean accept(ParameterMeta parameter) {
        return SUPPORTED_TYPES.contains(parameter.getActualType());
    }

    @Override
    public Object resolve(ParameterMeta parameter, HttpRequest request, HttpResponse response) {
        Class<?> type = parameter.getActualType();
        if (type == HttpRequest.class) {
            return request;
        }
        if (type == HttpResponse.class) {
            return response;
        }
        if (type == HttpMethods.class) {
            return HttpMethods.of(request.method());
        }
        if (type == Locale.class) {
            return request.locale();
        }
        if (type == InputStream.class) {
            return request.inputStream();
        }
        if (type == OutputStream.class) {
            return response.outputStream();
        }
        return null;
    }
}
