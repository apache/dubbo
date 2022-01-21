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
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.StreamMethodDescriptor;
import org.apache.dubbo.rpc.protocol.tri.DefaultFuture2;
import org.apache.dubbo.rpc.protocol.tri.GrpcStatus;
import org.apache.dubbo.rpc.protocol.tri.TripleConstant;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;
import org.apache.dubbo.rpc.protocol.tri.compressor.Compressor;
import org.apache.dubbo.rpc.protocol.tri.observer.CallToObserverAdapter;
import org.apache.dubbo.rpc.protocol.tri.observer.WrapperRequestObserver;
import org.apache.dubbo.rpc.protocol.tri.pack.GenericPack;
import org.apache.dubbo.rpc.protocol.tri.pack.GenericUnpack;
import org.apache.dubbo.rpc.protocol.tri.pack.PbPack;
import org.apache.dubbo.rpc.protocol.tri.pack.PbUnpack;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class ClientCall {
    private static final Logger logger = LoggerFactory.getLogger(ClientCall.class);
    private final Connection connection;
    private final ExecutorService executor;
    private final DefaultHttp2Headers headers;
    private final URL url;
    private final Compressor compressor;
    private final PbUnpack<?> unpack;
    private ClientStream stream;
    private boolean canceled;

    public ClientCall(URL url,
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
                      ExecutorService executor,
                      MethodDescriptor methodDescriptor
    ) {
        this.url = url;
        this.executor = executor;
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
            unpack = PbUnpack.RESP_PB_UNPACK;
        } else {
            unpack = new PbUnpack<>(methodDescriptor.getReturnClass());
        }
    }

    public static void call(ClientCall call,
                            long requestId,
                            Object[] arguments,
                            Connection connection,
                            MethodDescriptor methodDescriptor,
                            GenericPack genericPack,
                            List<String> argumentTypes,
                            GenericUnpack genericUnpack) {
        if (methodDescriptor instanceof StreamMethodDescriptor) {
            streamCall(call, requestId, arguments, connection, (StreamMethodDescriptor) methodDescriptor, genericPack, argumentTypes, genericUnpack);
        } else {
            Object argument;
            if (methodDescriptor.isNeedWrap()) {
                argument = arguments;
            } else {
                argument = arguments[0];
            }
            unaryCall(call, argument, requestId, connection, methodDescriptor, genericPack, argumentTypes, genericUnpack);
        }
    }

    public static void streamCall(ClientCall call,
                                  long requestId,
                                  Object[] arguments,
                                  Connection connection,
                                  StreamMethodDescriptor methodDescriptor,
                                  GenericPack genericPack, List<String> argumentTypes,
                                  GenericUnpack genericUnpack) {
        AppResponse appResponse = new AppResponse();
        Response response = new Response(requestId, TripleConstant.TRI_VERSION);
        response.setResult(appResponse);
        DefaultFuture2.received(connection, response);
        if (methodDescriptor.streamType == StreamMethodDescriptor.StreamType.CLIENT || methodDescriptor.streamType == StreamMethodDescriptor.StreamType.BI_DIRECTIONAL) {
            StreamObserver<Object> responseObserver = (StreamObserver<Object>) arguments[0];
            final StreamObserver<Object> requestObserver = streamCall(call, responseObserver, methodDescriptor, genericPack, argumentTypes, genericUnpack);
            appResponse.setValue(requestObserver);
        } else {
            Object request = arguments[0];
            StreamObserver<Object> responseObserver = (StreamObserver<Object>) arguments[1];
            final StreamObserver<Object> requestObserver = streamCall(call, responseObserver, methodDescriptor, genericPack, argumentTypes, genericUnpack);
            requestObserver.onNext(request);
            requestObserver.onCompleted();
        }
        DefaultFuture2.sent(requestId);
        DefaultFuture2.received(connection, response);
    }

    public static StreamObserver<Object> streamCall(ClientCall call,
                                                    StreamObserver<Object> responseObserver,
                                                    StreamMethodDescriptor methodDescriptor,
                                                    GenericPack genericPack, List<String> argumentTypes,
                                                    GenericUnpack genericUnpack) {
        ObserverToCallListenerAdapter listener = new ObserverToCallListenerAdapter(responseObserver);
        final StreamObserver<Object> requestObserver = call(call, methodDescriptor, listener, genericPack, argumentTypes, genericUnpack);
        return requestObserver;
    }

    public static void unaryCall(ClientCall call, Object request, long requestId, Connection connection,
                                 MethodDescriptor methodDescriptor,
                                 GenericPack genericPack, List<String> argumentTypes,
                                 GenericUnpack genericUnpack) {
        final UnaryCallListener listener = new UnaryCallListener(requestId, connection);
        final StreamObserver<Object> requestObserver = call(call, methodDescriptor, listener, genericPack, argumentTypes, genericUnpack);
        try {
            requestObserver.onNext(request);
            requestObserver.onCompleted();
        } catch (Throwable t) {
            cancelByThrowable(call, t);
        }
    }

    public static StreamObserver<Object> call(ClientCall call, MethodDescriptor methodDescriptor,
                                              ClientCall.Listener responseListener,
                                              GenericPack genericPack, List<String> argumentTypes,
                                              GenericUnpack genericUnpack) {

        if (methodDescriptor.isNeedWrap()) {
            return wrapperCall(call, responseListener, genericPack, argumentTypes, genericUnpack);
        } else {
            return call(call, responseListener);
        }
    }

    public static StreamObserver<Object> wrapperCall(ClientCall call, ClientCall.Listener responseListener,
                                                     GenericPack genericPack, List<String> argumentTypes,
                                                     GenericUnpack genericUnpack) {
        final StreamObserver<Object> requestObserver = WrapperRequestObserver.wrap(new CallToObserverAdapter(call), argumentTypes, genericPack);
        final Listener wrapResponseListener = WrapResponseCallListener.wrap(responseListener, genericUnpack);
        call.start(wrapResponseListener);
        return requestObserver;
    }

    public static StreamObserver<Object> call(ClientCall call, ClientCall.Listener responseListener) {
        final CallToObserverAdapter requestObserver = new CallToObserverAdapter(call);
        call.start(responseListener);
        return requestObserver;
    }

    public static void unaryCall(ClientCall call, Object request, long requestId, Connection connection, GenericUnpack unpack) {
        UnaryCallListener listener = new WrapUnaryResponseCallListener(requestId, connection, unpack);
        unaryCall(call, request, listener);
    }

    public static void unaryCall(ClientCall call, Object request, ClientCall.Listener listener) {
        startCall(call, listener);
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
            data = PbPack.INSTANCE.pack(message);
            final byte[] compress = compressor.compress(data);
            stream.writeMessage(compress);
        } catch (IOException e) {
            cancel("Serialize request failed", e);
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
            new ClientStreamListenerImpl(responseListener, unpack));
        stream.startCall(headers);
    }

    public void cancel(String message, Throwable t) {
        if (canceled) {
            return;
        }
        canceled = true;
        if (stream != null) {
            final GrpcStatus status = GrpcStatus.fromCode(GrpcStatus.Code.CANCELLED);
            if (message != null) {
                status.withDescription(message);
            } else {
                status.withDescription("Cancel by client without message");
            }
            if (t != null) {
                status.withCause(t);
            }
            stream.cancelByLocal(status);
        }

    }

    interface Listener {

        void onMessage(Object message);

        void onClose(GrpcStatus status, Map<String, Object> trailers);
    }

    class ClientStreamListenerImpl implements ClientStreamListener {
        private final Listener listener;
        private final PbUnpack<?> unpack;
        private boolean done;

        ClientStreamListenerImpl(Listener listener, PbUnpack<?> unpack) {
            this.unpack = unpack;
            this.listener = listener;
        }

        @Override
        public void onMessage(byte[] message) {
            executor.execute(()-> {
                try {
                    final Object unpacked = unpack.unpack(message);
                    listener.onMessage(unpacked);
                } catch (IOException e) {
                    cancelByErr(GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                        .withDescription("Deserialize response failed")
                        .withCause(e));
                }
            });
        }

        @Override
        public void complete(GrpcStatus grpcStatus, Map<String, Object> attachments) {
            executor.execute(()-> {
                if (done) {
                    return;
                }
                done = true;
                try {
                    listener.onClose(grpcStatus, attachments);
                } catch (Throwable t) {
                    cancelByErr(GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                        .withDescription("Close stream error")
                        .withCause(t));
                }
            });
        }

        void cancelByErr(GrpcStatus status) {
            stream.cancelByLocal(status);
        }

        @Override
        public void onHeaders(Http2Headers headers) {
            // ignored
        }
    }
}
