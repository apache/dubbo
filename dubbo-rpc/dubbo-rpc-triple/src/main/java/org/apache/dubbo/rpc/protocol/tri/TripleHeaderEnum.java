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
package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.netty.handler.codec.http2.Http2Headers;

public enum TripleHeaderEnum {
    AUTHORITY_KEY(":authority"),
    PATH_KEY(":path"),
    HTTP_STATUS_KEY("http-status"),
    STATUS_KEY("grpc-status"),
    MESSAGE_KEY("grpc-message"),
    STATUS_DETAIL_KEY("grpc-status-details-bin"),
    TIMEOUT("grpc-timeout"),
    CONTENT_TYPE_KEY("content-type"),
    CONTENT_PROTO("application/grpc+proto"),
    APPLICATION_GRPC("application/grpc"),
    GRPC_ENCODING("grpc-encoding"),
    GRPC_ACCEPT_ENCODING("grpc-accept-encoding"),
    CONSUMER_APP_NAME_KEY("tri-consumer-appname"),
    SERVICE_VERSION("tri-service-version"),
    SERVICE_GROUP("tri-service-group"),
    SERVICE_TIMEOUT("tri-service-timeout"),
    TRI_HEADER_CONVERT("tri-header-convert"),
    TRI_EXCEPTION_CODE("tri-exception-code");

    static final Map<String, TripleHeaderEnum> enumMap = new HashMap<>();

    static final Set<String> excludeAttachmentsSet = new HashSet<>();

    static {
        for (TripleHeaderEnum item : values()) {
            enumMap.put(item.getHeader(), item);
        }

        for (Http2Headers.PseudoHeaderName value : Http2Headers.PseudoHeaderName.values()) {
            excludeAttachmentsSet.add(value.value().toString());
        }

        String[] internalHttpHeaders = new String[] {
            CommonConstants.GROUP_KEY,
            CommonConstants.INTERFACE_KEY,
            CommonConstants.PATH_KEY,
            CommonConstants.REMOTE_APPLICATION_KEY,
            CommonConstants.APPLICATION_KEY,
            TripleConstant.SERIALIZATION_KEY,
            RestConstants.HEADER_SERVICE_VERSION,
            RestConstants.HEADER_SERVICE_GROUP
        };
        Collections.addAll(excludeAttachmentsSet, internalHttpHeaders);

        String[] excludeStandardHttpHeaders;
        if (TripleProtocol.PASS_THROUGH_STANDARD_HTTP_HEADERS) {
            excludeStandardHttpHeaders = new String[] {
                "accept",
                "accept-charset",
                "accept-encoding",
                "accept-language",
                "cache-control",
                "connection",
                "content-length",
                "content-md5",
                "content-type",
                "host"
            };
        } else {
            excludeStandardHttpHeaders = new String[] {
                "accept",
                "accept-charset",
                "accept-datetime",
                "accept-encoding",
                "accept-language",
                "access-control-request-headers",
                "access-control-request-method",
                "authorization",
                "cache-control",
                "connection",
                "content-length",
                "content-md5",
                "content-type",
                "cookie",
                "date",
                "dnt",
                "expect",
                "forwarded",
                "from",
                "host",
                "http2-settings",
                "if-match",
                "if-modified-since",
                "if-none-match",
                "if-range",
                "if-unmodified-since",
                "max-forwards",
                "origin",
                "pragma",
                "proxy-authorization",
                "range",
                "referer",
                "sec-fetch-dest",
                "sec-fetch-mode",
                "sec-fetch-site",
                "sec-fetch-user",
                "te",
                "trailer",
                "upgrade",
                "upgrade-insecure-requests",
                "user-agent",
                "x-csrf-token",
                "x-forwarded-for",
                "x-forwarded-host",
                "x-forwarded-proto",
                "x-http-method-override",
                "x-real-ip",
                "x-request-id",
                "x-requested-with"
            };
        }

        Collections.addAll(excludeAttachmentsSet, excludeStandardHttpHeaders);
    }

    private final String header;

    TripleHeaderEnum(String header) {
        this.header = header;
    }

    public static boolean containsExcludeAttachments(String key) {
        return excludeAttachmentsSet.contains(key) || enumMap.containsKey(key);
    }

    public String getHeader() {
        return header;
    }
}
