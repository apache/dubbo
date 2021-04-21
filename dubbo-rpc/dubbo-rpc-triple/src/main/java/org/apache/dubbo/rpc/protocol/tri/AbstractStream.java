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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import io.netty.handler.codec.http2.Http2Headers;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.serialize.MultipleSerialization;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.config.Constants;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.protocol.tri.GrpcStatus.Code;

public abstract class AbstractStream implements Stream {
    public static final boolean ENABLE_ATTACHMENT_WRAP = Boolean.parseBoolean(
        ConfigUtils.getProperty("triple.attachment", "false"));
    protected static final String DUPLICATED_DATA = "Duplicated data";
    private final URL url;
    private final MultipleSerialization multipleSerialization;
    private final StreamObserver<Object> streamObserver;
    private final TransportObserver transportObserver;
    private ServiceDescriptor serviceDescriptor;
    private MethodDescriptor methodDescriptor;
    private String serializeType;
    private StreamObserver<Object> streamSubscriber;
    private TransportObserver transportSubscriber;

    protected AbstractStream(URL url) {
        this.url = url;
        final String value = url.getParameter(Constants.MULTI_SERIALIZATION_KEY, CommonConstants.DEFAULT_KEY);
        this.multipleSerialization = ExtensionLoader.getExtensionLoader(MultipleSerialization.class)
            .getExtension(value);
        this.streamObserver = createStreamObserver();
        this.transportObserver = createTransportObserver();
    }

    public AbstractStream method(MethodDescriptor md) {
        this.methodDescriptor = md;
        return this;
    }

    protected abstract StreamObserver<Object> createStreamObserver();

    protected abstract TransportObserver createTransportObserver();

    public String getSerializeType() {
        return serializeType;
    }

    public AbstractStream serialize(String serializeType) {
        if (serializeType.equals("hessian4")) {
            serializeType = "hessian2";
        }
        this.serializeType = serializeType;
        return this;
    }

    public MultipleSerialization getMultipleSerialization() {
        return multipleSerialization;
    }

    public StreamObserver<Object> getStreamSubscriber() {
        return streamSubscriber;
    }

    public TransportObserver getTransportSubscriber() {
        return transportSubscriber;
    }

    public MethodDescriptor getMethodDescriptor() {
        return methodDescriptor;
    }

    public ServiceDescriptor getServiceDescriptor() {
        return serviceDescriptor;
    }

    public void setServiceDescriptor(ServiceDescriptor serviceDescriptor) {
        this.serviceDescriptor = serviceDescriptor;
    }

    public URL getUrl() {
        return url;
    }

    @Override
    public void subscribe(StreamObserver<Object> observer) {
        this.streamSubscriber = observer;
    }

    @Override
    public void subscribe(TransportObserver observer) {
        this.transportSubscriber = observer;
    }

    @Override
    public StreamObserver<Object> asStreamObserver() {
        return streamObserver;
    }

    @Override
    public TransportObserver asTransportObserver() {
        return transportObserver;
    }

    protected void transportError(GrpcStatus status) {
        Metadata metadata = new DefaultMetadata();
        metadata.put(TripleConstant.STATUS_KEY, Integer.toString(status.code.code));
        metadata.put(TripleConstant.MESSAGE_KEY, status.toMessage());
        getTransportSubscriber().tryOnMetadata(metadata, true);
    }

    protected void transportError(Throwable throwable) {
        Metadata metadata = new DefaultMetadata();
        metadata.put(TripleConstant.STATUS_KEY, Integer.toString(Code.UNKNOWN.code));
        metadata.put(TripleConstant.MESSAGE_KEY, throwable.getMessage());
        getTransportSubscriber().tryOnMetadata(metadata, true);
    }

    protected Map<String, Object> parseMetadataToMap(Metadata metadata) {
        Map<String, Object> attachments = new HashMap<>();
        for (Map.Entry<CharSequence, CharSequence> header : metadata) {
            String key = header.getKey().toString();
            if (Http2Headers.PseudoHeaderName.isPseudoHeader(key)) {
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

    protected void convertAttachment(Metadata metadata, Map<String, Object> attachments) {
        for (Map.Entry<String, Object> entry : attachments.entrySet()) {
            final String key = entry.getKey().toLowerCase(Locale.ROOT);
            if (Http2Headers.PseudoHeaderName.isPseudoHeader(key)) {
                continue;
            }
            final Object v = entry.getValue();
            convertSingleAttachment(metadata, key, v);
        }
    }

    private void convertSingleAttachment(Metadata metadata, String key, Object v) {
        try {
            if (!ENABLE_ATTACHMENT_WRAP) {
                if (v instanceof String) {
                    metadata.put(key, (String)v);
                } else if (v instanceof byte[]) {
                    metadata.put(key + "-bin", TripleUtil.encodeBase64ASCII((byte[])v));
                }
            } else {
                if (v instanceof String || serializeType == null) {
                    metadata.put(key, v.toString());
                } else {
                    String encoded = TripleUtil.encodeWrapper(url, v, this.serializeType, getMultipleSerialization());
                    metadata.put(key + "-tw-bin", encoded);
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Meet exception when convert single attachment key:" + key, e);
        }
    }

    protected static abstract class AbstractTransportObserver implements TransportObserver {
        private Metadata headers;
        private Metadata trailers;

        public Metadata getHeaders() {
            return headers;
        }

        public Metadata getTrailers() {
            return trailers;
        }

        @Override
        public void onMetadata(Metadata metadata, boolean endStream, OperationHandler handler) {
            if (headers == null) {
                headers = metadata;
            } else {
                trailers = metadata;
            }
        }

    }

    protected abstract static class UnaryTransportObserver extends AbstractTransportObserver
        implements TransportObserver {
        private byte[] data;

        public byte[] getData() {
            return data;
        }

        @Override
        public void onData(byte[] in, boolean endStream, OperationHandler handler) {
            if (data == null) {
                this.data = in;
            } else {
                handler.operationDone(OperationResult.FAILURE, GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                    .withDescription(DUPLICATED_DATA).asException());
            }
        }
    }
}
