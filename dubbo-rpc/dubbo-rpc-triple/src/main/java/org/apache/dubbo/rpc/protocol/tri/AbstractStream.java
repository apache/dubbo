/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  * contributor license agreements.  See the NOTICE file distributed with
 *  * this work for additional information regarding copyright ownership.
 *  * The ASF licenses this file to You under the Apache License, Version 2.0
 *  * (the "License"); you may not use this file except in compliance with
 *  * the License.  You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.apache.dubbo.rpc.protocol.tri;


import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.serialize.MultipleSerialization;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.config.Constants;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2Headers;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.apache.dubbo.rpc.protocol.tri.TripleUtil.responseErr;

public abstract class AbstractStream implements Stream {
    public static final boolean ENABLE_ATTACHMENT_WRAP = Boolean.parseBoolean(ConfigUtils.getProperty("triple.attachment", "false"));
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStream.class);
    private static final GrpcStatus TOO_MANY_DATA = GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
            .withDescription("Too many data");
    private final ChannelHandlerContext ctx;
    private final URL url;
    private MultipleSerialization multipleSerialization;
    private Http2Headers headers;
    private Http2Headers te;
    private boolean needWrap;
    private InputStream data;
    private String serializeType;

    protected AbstractStream(URL url, ChannelHandlerContext ctx) {
        this(url, ctx, false);
    }

    protected AbstractStream(URL url, ChannelHandlerContext ctx, boolean needWrap) {
        this.ctx = ctx;
        this.url = url;
        this.needWrap = needWrap;
        if (needWrap) {
            loadFromURL(url);
        }
    }

    protected void loadFromURL(URL url) {
        final String value = url.getParameter(Constants.MULTI_SERIALIZATION_KEY, "default");
        this.multipleSerialization = ExtensionLoader.getExtensionLoader(MultipleSerialization.class).getExtension(value);
    }

    public URL getUrl() {
        return url;
    }

    public String getSerializeType() {
        return serializeType;
    }

    protected void setSerializeType(String serializeType) {
        this.serializeType = serializeType;
    }

    protected boolean isNeedWrap() {
        return needWrap;
    }

    protected void setNeedWrap(boolean needWrap) {
        this.needWrap = needWrap;
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public Http2Headers getHeaders() {
        return headers;
    }

    public Http2Headers getTe() {
        return te;
    }

    public InputStream getData() {
        return data;
    }

    public MultipleSerialization getMultipleSerialization() {
        return multipleSerialization;
    }

    @Override
    public void onData(InputStream in) {
        if (data != null) {
            responseErr(ctx, TOO_MANY_DATA);
            return;
        }

        this.data = in;
    }

    public void onHeaders(Http2Headers headers) {
        if (this.headers == null) {
            this.headers = headers;
        } else if (te == null) {
            this.te = headers;
        }
    }

    protected Map<String, Object> parseHeadersToMap(Http2Headers headers) {
        Map<String, Object> attachments = new HashMap<>();
        for (Map.Entry<CharSequence, CharSequence> header : headers) {
            String key = header.getKey().toString();

            if (ENABLE_ATTACHMENT_WRAP) {
                if (key.endsWith("-tw-bin") && key.length() > 7) {
                    try {
                        attachments.put(key.substring(0, key.length() - 7), TripleUtil.decodeObjFromHeader(getUrl(), header.getValue(), getMultipleSerialization()));
                    } catch (Exception e) {
                        LOGGER.error("Failed to parse response attachment key=" + key, e);
                    }
                }
            }
            if (key.endsWith("-bin") && key.length() > 4) {
                try {
                    attachments.put(key.substring(0, key.length() - 4), TripleUtil.decodeASCIIByte(header.getValue()));
                } catch (Exception e) {
                    LOGGER.error("Failed to parse response attachment key=" + key, e);
                }
            } else {
                attachments.put(key, header.getValue().toString());
            }
        }
        return attachments;
    }

    protected void convertAttachment(Http2Headers trailers, Map<String, Object> attachments) throws IOException {
        for (Map.Entry<String, Object> entry : attachments.entrySet()) {
            final String key = entry.getKey().toLowerCase(Locale.ROOT);
            final Object v = entry.getValue();
            if (!ENABLE_ATTACHMENT_WRAP) {
                if (v instanceof String) {
                    trailers.addObject(key, v);
                } else if (v instanceof byte[]) {
                    trailers.addObject(key + "-bin", TripleUtil.encodeBase64((byte[]) v));
                }
            } else {
                if (v instanceof String || serializeType == null) {
                    trailers.addObject(key, v);
                } else {
                    String encoded = TripleUtil.encodeWrapper(url, v, this.serializeType, getMultipleSerialization());
                    trailers.add(key + "-tw-bin", encoded);
                }
            }
        }
    }
}
