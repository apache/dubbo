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

import org.apache.dubbo.rpc.CancellationContext;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.PackableMethod;
import org.apache.dubbo.rpc.protocol.tri.compressor.Compressor;
import org.apache.dubbo.rpc.protocol.tri.compressor.Identity;
import org.apache.dubbo.rpc.protocol.tri.stream.StreamUtils;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.util.AsciiString;

import java.util.Map;

public class RequestMetadata {

    public AsciiString scheme;
    public String application;
    public String service;
    public String version;
    public String group;
    public String address;
    public String acceptEncoding;
    public String timeout;
    public Compressor compressor;
    public CancellationContext cancellationContext;
    public MethodDescriptor method;
    public PackableMethod packableMethod;
    public Map<String, Object> attachments;
    public boolean convertNoLowerHeader;
    public boolean ignoreDefaultVersion;

    public DefaultHttp2Headers toHeaders() {
        DefaultHttp2Headers header = new DefaultHttp2Headers(false);
        header.scheme(scheme)
            .authority(address)
            .method(HttpMethod.POST.asciiName())
            .path("/" + service + "/" + method.getMethodName())
            .set(TripleHeaderEnum.CONTENT_TYPE_KEY.getHeader(), TripleConstant.CONTENT_PROTO)
            .set(HttpHeaderNames.TE, HttpHeaderValues.TRAILERS);
        setIfNotNull(header, TripleHeaderEnum.TIMEOUT.getHeader(), timeout);
        if (!ignoreDefaultVersion || !"1.0.0".equals(version)) {
            setIfNotNull(header, TripleHeaderEnum.SERVICE_VERSION.getHeader(), version);
        }
        setIfNotNull(header, TripleHeaderEnum.SERVICE_GROUP.getHeader(), group);
        setIfNotNull(header, TripleHeaderEnum.CONSUMER_APP_NAME_KEY.getHeader(),
            application);
        setIfNotNull(header, TripleHeaderEnum.GRPC_ACCEPT_ENCODING.getHeader(),
            acceptEncoding);
        if (!Identity.MESSAGE_ENCODING.equals(compressor.getMessageEncoding())) {
            setIfNotNull(header, TripleHeaderEnum.GRPC_ENCODING.getHeader(),
                compressor.getMessageEncoding());
        }
        StreamUtils.convertAttachment(header, attachments, convertNoLowerHeader);
        return header;
    }

    private void setIfNotNull(DefaultHttp2Headers headers, CharSequence key,
        CharSequence value) {
        if (value == null) {
            return;
        }
        headers.set(key, value);
    }


}
