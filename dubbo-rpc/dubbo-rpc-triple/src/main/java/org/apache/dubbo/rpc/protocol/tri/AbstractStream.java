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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.util.internal.shaded.org.jctools.queues.MpscChunkedArrayQueue;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.serialize.MultipleSerialization;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.config.Constants;
import org.apache.dubbo.rpc.model.MethodDescriptor;

public abstract class AbstractStream<T> implements Stream<T> {
    public static final boolean ENABLE_ATTACHMENT_WRAP = Boolean.parseBoolean(
        ConfigUtils.getProperty("triple.attachment", "false"));
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStream.class);
    private static final GrpcStatus TOO_MANY_DATA = GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
        .withDescription("Too many data");
    private final ChannelHandlerContext ctx;
    private final URL url;
    private final MethodDescriptor md;
    private final Queue<InputStream> datas;
    private MultipleSerialization multipleSerialization;
    private Http2Headers headers;
    private Http2Headers te;
    private String serializeType;
    private StreamObserver<Object> observer;
    private Processor processor;
    private final AtomicBoolean canceled = new AtomicBoolean(false);

    protected AbstractStream(URL url, ChannelHandlerContext ctx, MethodDescriptor md) {
        this.ctx = ctx;
        this.url = url;
        this.md = md;
        this.datas = new MpscChunkedArrayQueue<>(16, 1 << 30);
        if (md.isNeedWrap()) {
            loadFromURL(url);
        }
    }

    protected void loadFromURL(URL url) {
        final String value = url.getParameter(Constants.MULTI_SERIALIZATION_KEY, "default");
        this.multipleSerialization = ExtensionLoader.getExtensionLoader(MultipleSerialization.class).getExtension(
            value);
    }

    public Processor getProcessor() {
        return processor;
    }

    public void setProcessor(Processor processor) {
        this.processor = processor;
    }

    protected AtomicBoolean getCanceled() {
        return canceled;
    }

    public MethodDescriptor getMd() {
        return md;
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

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public Http2Headers getHeaders() {
        return headers;
    }

    public Http2Headers getTe() {
        return te;
    }

    public MultipleSerialization getMultipleSerialization() {
        return multipleSerialization;
    }

    @Override
    public void onData(InputStream in) {
        onSingleMessage(in);
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
            if(Http2Headers.PseudoHeaderName.isPseudoHeader(key)){
                continue;
            }

            if (ENABLE_ATTACHMENT_WRAP) {
                if (key.endsWith("-tw-bin") && key.length() > 7) {
                    try {
                        attachments.put(key.substring(0, key.length() - 7),
                            TripleUtil.decodeObjFromHeader(url, header.getValue(), multipleSerialization));
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

    protected void convertAttachment(Http2Headers trailers, Map<String, Object> attachments) {
        for (Map.Entry<String, Object> entry : attachments.entrySet()) {
            final String key = entry.getKey().toLowerCase(Locale.ROOT);
            if(Http2Headers.PseudoHeaderName.isPseudoHeader(key)){
                continue;
            }
            final Object v = entry.getValue();
            convertSingleAttachment(trailers, key, v);
        }
    }

    private void convertSingleAttachment(Http2Headers trailers, String key, Object v) {
        try {
            if (!ENABLE_ATTACHMENT_WRAP) {
                if (v instanceof String) {
                    trailers.addObject(key, v);
                } else if (v instanceof byte[]) {
                    trailers.add(key + "-bin", TripleUtil.encodeBase64ASCII((byte[])v));
                }
            } else {
                if (v instanceof String || serializeType == null) {
                    trailers.addObject(key, v);
                } else {
                    String encoded = TripleUtil.encodeWrapper(url, v, this.serializeType, getMultipleSerialization());
                    trailers.add(key + "-tw-bin", encoded);
                }
            }
        } catch (IOException e) {
            // todo log
        }
    }

    protected void streamCreated(Object msg, ChannelPromise promise) {}

    protected void onSingleMessage(InputStream in) {}

    protected StreamObserver<Object> getObserver() {
        return observer;
    }

    protected void setObserver(StreamObserver<Object> observer) {
        this.observer = observer;
    }
}
