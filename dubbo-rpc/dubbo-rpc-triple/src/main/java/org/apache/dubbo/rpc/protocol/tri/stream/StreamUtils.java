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

package org.apache.dubbo.rpc.protocol.tri.stream;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.protocol.tri.RequestMetadata;
import org.apache.dubbo.rpc.protocol.tri.TripleConstant;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;
import org.apache.dubbo.rpc.protocol.tri.compressor.Compressor;
import org.apache.dubbo.rpc.protocol.tri.compressor.Identity;
import org.apache.dubbo.rpc.protocol.tri.pack.GenericPack;
import org.apache.dubbo.rpc.protocol.tri.pack.GenericUnpack;
import org.apache.dubbo.rpc.protocol.tri.transport.H2TransportListener;
import org.apache.dubbo.rpc.support.RpcUtils;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.util.AsciiString;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

public class StreamUtils {
    protected static final Logger LOGGER = LoggerFactory.getLogger(StreamUtils.class);

    public static RequestMetadata createRequest(URL url, MethodDescriptor methodDescriptor, Invocation invocation,
                                                long requestId, Compressor compressor, String acceptEncoding,
                                                int timeout, GenericPack genericPack, GenericUnpack genericUnpack) {
        final String methodName = RpcUtils.getMethodName(invocation);
        final RequestMetadata meta = new RequestMetadata();
        meta.scheme = getSchemeFromUrl(url);
        meta.requestId = requestId;
        String application = (String) invocation.getObjectAttachments().get(CommonConstants.APPLICATION_KEY);
        if (application == null) {
            application = (String) invocation.getObjectAttachments().get(CommonConstants.REMOTE_APPLICATION_KEY);
        }
        meta.application = application;

        meta.method = methodDescriptor;
        if (meta.method == null) {
            throw new IllegalStateException("MethodDescriptor not found for" + methodName + " params:" + Arrays.toString(invocation.getCompatibleParamSignatures()));
        }
        meta.attachments = invocation.getObjectAttachments();
        meta.compressor = compressor;
        meta.acceptEncoding = acceptEncoding;
        meta.address = url.getAddress();
        meta.service = url.getPath();
        meta.group = url.getGroup();
        meta.version = url.getVersion();
        meta.timeout = timeout + "m";
        meta.genericPack = genericPack;
        meta.genericUnpack = genericUnpack;
        meta.arguments = invocation.getArguments();
        return meta;
    }

    public static DefaultHttp2Headers metadataToHeaders(RequestMetadata metadata) {
        DefaultHttp2Headers header = new DefaultHttp2Headers(false);
        header.scheme(metadata.scheme)
            .authority(metadata.address)
            .method(HttpMethod.POST.asciiName())
            .path("/" + metadata.service + "/" + metadata.method.getMethodName())
            .set(TripleHeaderEnum.CONTENT_TYPE_KEY.getHeader(), TripleConstant.CONTENT_PROTO)
            .set(HttpHeaderNames.TE, HttpHeaderValues.TRAILERS);
        setIfNotNull(header, TripleHeaderEnum.TIMEOUT.getHeader(), metadata.timeout);
        if (!"1.0.0".equals(metadata.version)) {
            setIfNotNull(header, TripleHeaderEnum.SERVICE_VERSION.getHeader(), metadata.version);
        }
        setIfNotNull(header, TripleHeaderEnum.SERVICE_GROUP.getHeader(), metadata.group);
        setIfNotNull(header, TripleHeaderEnum.CONSUMER_APP_NAME_KEY.getHeader(), metadata.application);
        setIfNotNull(header, TripleHeaderEnum.GRPC_ACCEPT_ENCODING.getHeader(), metadata.acceptEncoding);
        if (!Identity.MESSAGE_ENCODING.equals(metadata.compressor.getMessageEncoding())) {
            setIfNotNull(header, TripleHeaderEnum.GRPC_ENCODING.getHeader(), metadata.compressor.getMessageEncoding());
        }
        StreamUtils.convertAttachment(header, metadata.attachments);
        return header;
    }

    private static void setIfNotNull(DefaultHttp2Headers headers, CharSequence key, CharSequence value) {
        if (value == null) {
            return;
        }
        headers.set(key, value);
    }

    private static AsciiString getSchemeFromUrl(URL url) {
        boolean ssl = url.getParameter(CommonConstants.SSL_ENABLED_KEY, false);
        return ssl ? TripleConstant.HTTPS_SCHEME : TripleConstant.HTTP_SCHEME;
    }

    /**
     * Parse and put the KV pairs into metadata. Ignore Http2 PseudoHeaderName and internal name.
     * Only raw byte array or string value will be put.
     *
     * @param headers     the metadata holder
     * @param attachments KV pairs
     */
    public static void convertAttachment(DefaultHttp2Headers headers, Map<String, Object> attachments) {
        if (attachments == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : attachments.entrySet()) {
            final String key = entry.getKey().toLowerCase(Locale.ROOT);
            if (Http2Headers.PseudoHeaderName.isPseudoHeader(key)) {
                continue;
            }
            if (TripleHeaderEnum.containsExcludeAttachments(key)) {
                continue;
            }
            final Object v = entry.getValue();
            convertSingleAttachment(headers, key, v);
        }
    }

    /**
     * Convert each user's attach value to metadata
     *
     * @param headers outbound headers
     * @param key     metadata key
     * @param v       metadata value (Metadata Only string and byte arrays are allowed)
     */
    private static void convertSingleAttachment(DefaultHttp2Headers headers, String key, Object v) {
        try {
            // todo Support boolean/ numbers
            if (v instanceof String) {
                String str = (String) v;
                headers.set(key, str);
            } else if (v instanceof byte[]) {
                String str = H2TransportListener.encodeBase64ASCII((byte[]) v);
                headers.set(key + TripleConstant.GRPC_BIN_SUFFIX, str);
            }
        } catch (Throwable t) {
            LOGGER.warn("Meet exception when convert single attachment key:" + key + " value=" + v, t);
        }
    }


}
