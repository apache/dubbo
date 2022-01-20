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

package org.apache.dubbo.rpc.protocol.tri.call;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.remoting.api.Connection;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.StreamMethodDescriptor;
import org.apache.dubbo.rpc.protocol.tri.GrpcStatus;
import org.apache.dubbo.rpc.protocol.tri.TripleConstant;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;
import org.apache.dubbo.rpc.protocol.tri.compressor.Compressor;
import org.apache.dubbo.rpc.protocol.tri.observer.CallToObserverAdapter;
import org.apache.dubbo.rpc.protocol.tri.observer.UnaryObserver;
import org.apache.dubbo.rpc.protocol.tri.pack.GenericPack;
import org.apache.dubbo.rpc.protocol.tri.pack.GenericUnpack;
import org.apache.dubbo.rpc.protocol.tri.pack.Pack;
import org.apache.dubbo.rpc.protocol.tri.pack.PbPack;
import org.apache.dubbo.rpc.protocol.tri.pack.PbUnpack;
import org.apache.dubbo.rpc.protocol.tri.pack.Unpack;
import org.apache.dubbo.rpc.protocol.tri.pack.VoidUnpack;
import org.apache.dubbo.rpc.protocol.tri.pack.WrapReqPack;
import org.apache.dubbo.rpc.protocol.tri.pack.WrapRespUnpack;
import org.apache.dubbo.rpc.protocol.tri.stream.ClientStream;
import org.apache.dubbo.rpc.protocol.tri.stream.ClientStreamListener;
import org.apache.dubbo.rpc.protocol.tri.stream.StreamUtils;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.util.AsciiString;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class ClientCall {
    private static final Logger logger = LoggerFactory.getLogger(ClientCall.class);
    public final long requestId;
    private final Connection connection;
    private final MethodDescriptor methodDescriptor;
    private final ExecutorService executor;
    private final DefaultHttp2Headers headers;
    private final URL url;
    private final Pack requestPack;
    private final Unpack responseUnpack;
    private final Compressor compressor;
    private ClientStream stream;

    public ClientCall(URL url,
                      long requestId,
                      Connection connection,
                      AsciiString scheme,
                      String service,
                      String serviceVersion,
                      String serviceGroup,
                      String application,
                      String authority,
                      String timeout,
                      String methodName,
                      String acceptEncoding,
                      Compressor compressor,
                      Map<String, Object> attachments,
                      Class<?>[] parameterTypes,
                      GenericPack genericPack,
                      GenericUnpack genericUnpack,
                      ExecutorService executor,
                      MethodDescriptor methodDescriptor
    ) {
        this.url = url;
        this.requestId = requestId;
        this.executor = executor;
        this.methodDescriptor = methodDescriptor;
        this.connection = connection;
        this.compressor = compressor;
        this.headers = new DefaultHttp2Headers(false);
        this.headers.scheme(scheme)
            .authority(authority)
            .method(HttpMethod.POST.asciiName())
            .path("/" + service + "/" + methodName)
            .set(TripleHeaderEnum.CONTENT_TYPE_KEY.getHeader(), TripleConstant.CONTENT_PROTO)
            .set(HttpHeaderNames.TE, HttpHeaderValues.TRAILERS);
        setIfNotNull(headers, TripleHeaderEnum.TIMEOUT.getHeader(), timeout);
        setIfNotNull(headers, TripleHeaderEnum.SERVICE_VERSION.getHeader(), serviceVersion);
        setIfNotNull(headers, TripleHeaderEnum.SERVICE_GROUP.getHeader(), serviceGroup);
        setIfNotNull(headers, TripleHeaderEnum.CONSUMER_APP_NAME_KEY.getHeader(), application);
        setIfNotNull(headers, TripleHeaderEnum.GRPC_ENCODING.getHeader(), compressor.getMessageEncoding());
        setIfNotNull(headers, TripleHeaderEnum.GRPC_ACCEPT_ENCODING.getHeader(), acceptEncoding);
        if (attachments != null) {
            StreamUtils.convertAttachment(headers, attachments);
        }
        if (methodDescriptor.isNeedWrap()) {
            requestPack = new WrapReqPack(parameterTypes, genericPack, PbPack.INSTANCE);
            if (!Void.TYPE.equals(methodDescriptor.getReturnClass())) {
                responseUnpack = new WrapRespUnpack(genericUnpack);
            } else {
                responseUnpack = VoidUnpack.INSTANCE;
            }
        } else {
            requestPack = PbPack.INSTANCE;
            responseUnpack = new PbUnpack(methodDescriptor.getReturnClass());
        }
    }

    public static StreamObserver<Object> streamCall(ClientCall call, StreamObserver<Object> responseObserver) {
        final CallToObserverAdapter requestObserver = new CallToObserverAdapter(call);
        startCall(call, new ObserverToCallListenerAdaptor(call.requestId, responseObserver, true));
        return requestObserver;
    }

    public static void unaryCall(ClientCall call, Object request) {
        UnaryObserver observer = new UnaryObserver(call.connection);
        unaryCall(call, request, observer, false);
    }

    public static void unaryCall(ClientCall call, Object request, StreamObserver<Object> responseObserver, boolean streamingMethod) {
        startCall(call, new ObserverToCallListenerAdaptor(call.requestId, responseObserver, streamingMethod));
        try {
            call.sendMessage(request);
            call.closeLocal();
        } catch (Throwable t) {
            cancelByThrowable(call, t);
        }
    }

    static void cancelByThrowable(ClientCall call, Throwable t) {
        try {
            call.cancel(null, t);
        } catch (Throwable t1) {
            logger.error("Cancel triple request failed", t1);
        }
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        } else {
            throw (Error) t;
        }
    }

    static void startCall(ClientCall call, Listener responseListener) {
        call.start(responseListener);
    }

    public static StreamObserver<Object> getObserver(MethodDescriptor methodDescriptor, Object[] arguments) {
        final int index = ((StreamMethodDescriptor) methodDescriptor).responseObserverIndex();
        return (StreamObserver<Object>) arguments[index];
    }

    private void setIfNotNull(DefaultHttp2Headers headers, CharSequence key, CharSequence value) {
        if (value == null) {
            return;
        }
        headers.set(key, value);
    }

    public void sendMessage(Object message) {
        final byte[] data;
        try {
            data = requestPack.pack(message);
            final byte[] compress = compressor.compress(data);
            stream.writeMessage(compress);
        } catch (IOException e) {
            cancel(GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                .withDescription("Write message failed")
                .withCause(e), e);
        }
    }

    public void closeLocal() {
        stream.complete();
    }

    public void start(Listener responseListener) {
        this.stream = new ClientStream(
            url,
            executor,
            connection.getChannel(),
            new ClientStreamListenerImpl(responseListener, responseUnpack));
        stream.startCall(headers);
    }

    public void cancel(GrpcStatus status, Throwable t) {

    }

    interface Listener {

        void onMessage(Object message);

        void onClose(GrpcStatus status, Map<String, Object> trailers);
    }

    static class ClientStreamListenerImpl implements ClientStreamListener {
        private final Listener listener;
        private final Unpack unpack;

        ClientStreamListenerImpl(Listener listener, Unpack unpack) {
            this.unpack = unpack;
            this.listener = listener;
        }

        @Override
        public void onMessage(byte[] message) {
            try {
                final Object unpack = this.unpack.unpack(message);
                listener.onMessage(unpack);
            } catch (IOException | ClassNotFoundException e) {
                complete(GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                    .withDescription("Deserialize response failed")
                    .withCause(e), null);
            }
        }

        @Override
        public void complete(GrpcStatus grpcStatus, Map<String, Object> attachments) {
            listener.onClose(grpcStatus, attachments);
        }

        @Override
        public void onHeaders(Http2Headers headers) {
            // ignored
        }
    }
}
