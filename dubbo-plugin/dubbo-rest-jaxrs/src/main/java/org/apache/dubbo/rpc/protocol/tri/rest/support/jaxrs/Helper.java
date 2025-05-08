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

import org.apache.dubbo.remoting.http12.HttpCookie;
import org.apache.dubbo.remoting.http12.HttpResult;
import org.apache.dubbo.remoting.http12.HttpUtils;
import org.apache.dubbo.remoting.http12.message.DefaultHttpResult.Builder;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.AnnotationMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import java.util.List;
import java.util.stream.Collectors;

public final class Helper {

    private Helper() {}

    public static boolean isRequired(ParameterMeta parameter) {
        return parameter.isHierarchyAnnotated(Annotations.Nonnull);
    }

    public static String defaultValue(ParameterMeta parameter) {
        AnnotationMeta<?> meta = parameter.findAnnotation(Annotations.DefaultValue);
        return meta == null ? null : meta.getValue();
    }

    public static HttpResult<Object> toBody(Response response) {
        Builder<Object> builder = HttpResult.builder().status(response.getStatus());
        if (response.hasEntity()) {
            builder.body(response.getEntity());
        }
        builder.headers(response.getStringHeaders());
        return builder.build();
    }

    public static MediaType toMediaType(String mediaType) {
        if (mediaType == null) {
            return null;
        }
        int index = mediaType.indexOf('/');
        if (index == -1) {
            return null;
        }
        return new MediaType(mediaType.substring(0, index), mediaType.substring(index + 1));
    }

    public static String toString(MediaType mediaType) {
        return mediaType.getType() + '/' + mediaType.getSubtype();
    }

    public static List<MediaType> toMediaTypes(String accept) {
        return HttpUtils.parseAccept(accept).stream().map(Helper::toMediaType).collect(Collectors.toList());
    }

    public static NewCookie convert(HttpCookie cookie) {
        return new NewCookie(
                cookie.name(),
                cookie.value(),
                cookie.path(),
                cookie.domain(),
                null,
                (int) cookie.maxAge(),
                cookie.secure(),
                cookie.httpOnly());
    }
}
