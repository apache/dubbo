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
import org.apache.dubbo.remoting.http12.HttpCookie;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.remoting.http12.rest.ParamType;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.NamedValueMeta;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Activate(onClass = "javax.ws.rs.CookieParam")
public class CookieParamArgumentResolver extends AbstractJaxrsArgumentResolver {

    @Override
    public Class<Annotation> accept() {
        return Annotations.CookieParam.type();
    }

    @Override
    protected ParamType getParamType(NamedValueMeta meta) {
        return ParamType.Cookie;
    }

    @Override
    protected Object resolveValue(NamedValueMeta meta, HttpRequest request, HttpResponse response) {
        return request.cookie(meta.name());
    }

    @Override
    protected Object resolveCollectionValue(NamedValueMeta meta, HttpRequest request, HttpResponse response) {
        Collection<HttpCookie> cookies = request.cookies();
        if (cookies.isEmpty()) {
            return Collections.emptyList();
        }
        String name = meta.name();
        List<HttpCookie> result = new ArrayList<>(cookies.size());
        for (HttpCookie cookie : cookies) {
            if (name.equals(cookie.name())) {
                result.add(cookie);
            }
        }
        return result;
    }

    @Override
    protected Object resolveMapValue(NamedValueMeta meta, HttpRequest request, HttpResponse response) {
        Collection<HttpCookie> cookies = request.cookies();
        if (cookies.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, List<HttpCookie>> mapValue = CollectionUtils.newLinkedHashMap(cookies.size());
        for (HttpCookie cookie : cookies) {
            mapValue.computeIfAbsent(cookie.name(), k -> new ArrayList<>()).add(cookie);
        }
        return mapValue;
    }
}
