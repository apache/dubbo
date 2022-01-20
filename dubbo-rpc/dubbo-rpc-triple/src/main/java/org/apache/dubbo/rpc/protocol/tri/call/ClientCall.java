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

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.serialize.MultipleSerialization;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.remoting.api.Connection;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.StreamMethodDescriptor;
import org.apache.dubbo.rpc.protocol.tri.GrpcStatus;
import org.apache.dubbo.rpc.protocol.tri.compressor.Compressor;
import org.apache.dubbo.rpc.protocol.tri.observer.CallToObserverAdapter;
import org.apache.dubbo.rpc.protocol.tri.observer.UnaryObserver;
import org.apache.dubbo.rpc.protocol.tri.pack.Pack;
import org.apache.dubbo.rpc.protocol.tri.pack.PbPack;
import org.apache.dubbo.rpc.protocol.tri.pack.PbUnpack;
import org.apache.dubbo.rpc.protocol.tri.pack.Unpack;
import org.apache.dubbo.rpc.protocol.tri.pack.VoidUnpack;
import org.apache.dubbo.rpc.protocol.tri.pack.WrapReqPack;
import org.apache.dubbo.rpc.protocol.tri.pack.WrapRespUnpack;
import org.apache.dubbo.rpc.protocol.tri.stream.ClientRequestObserver;
import org.apache.dubbo.rpc.protocol.tri.stream.ClientStream;
import org.apache.dubbo.rpc.protocol.tri.stream.ClientStreamListener;
import org.apache.dubbo.rpc.protocol.tri.stream.StreamClientListener;
import org.apache.dubbo.rpc.protocol.tri.stream.UnaryClientListener;

import java.util.Map;
import java.util.concurrent.ExecutorService;

public class ClientCall {
    private static final Logger logger = LoggerFactory.getLogger(ClientCall.class);
    public final long requestId;
    private final Connection connection;
    private final Request request;
    private final MethodDescriptor methodDescriptor;
    private final ExecutorService executor;
    private ClientStream stream;
    private Pack requestPack;
    private Unpack responseUnpack;
    private Compressor compressor;

    public ClientCall(Request request,
                      Connection connection,
                      String serviceVersion,
                      String serviceGroup,
                      String scheme,
                      ExecutorService executor,
                      MethodDescriptor methodDescriptor,
                      MultipleSerialization serialization,
                      String serializationName,
                      Compressor defaultCompressor,
                      String acceptEncoding,
                      ) {
        this.requestId = request.getId();
        this.executor=executor;
        this.methodDescriptor=methodDescriptor;
        this.request = request;
        this.connection = connection;
        this.scheme = getSchemeFromUrl(url);
        this.acceptEncoding = acceptEncoding;
        this.stream = new ClientStream(
            getUrl(),
            executor,
            id,
            connection.getChannel(),
            scheme,
            "/" + getUrl().getPath() + "/" + methodName,
            getUrl().getVersion(),
            getUrl().getGroup(),
            application,
            getUrl().getAddress(),
            compressor.getMessageEncoding(),
            acceptEncoding,
            timeout,
            compressor,
            invocation.getObjectAttachments(),
            requestPack,
            responseUnpack,
            listener);
    }

    private ClientStream createStream(RpcInvocation invocation, long id, String methodName, String timeout, ExecutorService executor) {
        String application = (String) invocation.getObjectAttachments().get(CommonConstants.APPLICATION_KEY);
        if (application == null) {
            application = (String) invocation.getObjectAttachments().get(CommonConstants.REMOTE_APPLICATION_KEY);
        }

        ClientStream stream ;

        Pack requestPack;
        Unpack responseUnpack;
        if (methodDescriptor.isNeedWrap()) {
            requestPack = new WrapReqPack(invocation.getParameterTypes(), genericPack, PbPack.INSTANCE);
            if (!Void.TYPE.equals(methodDescriptor.getReturnClass())) {
                responseUnpack = new WrapRespUnpack(genericUnpack);
            } else {
                responseUnpack = VoidUnpack.INSTANCE;
            }
        } else {
            requestPack = PbPack.INSTANCE;
            responseUnpack = new PbUnpack(methodDescriptor.getReturnClass());
        }
        ClientStreamListener listener;
        if (methodDescriptor instanceof StreamMethodDescriptor) {

            listener = new StreamClientListener(connection,id,(StreamObserver<Object>) responseObserver);
        } else {
            listener = new UnaryClientListener(connection, id);
        }

        if (methodDescriptor instanceof StreamMethodDescriptor) {
            ((StreamClientListener)listener).setRequestObserver(new ClientRequestObserver(stream));
        }
        return stream;
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

    }

    public static StreamObserver<Object> getObserver(MethodDescriptor methodDescriptor, Object[] arguments) {
        final int index = ((StreamMethodDescriptor) methodDescriptor).responseObserverIndex();
        return (StreamObserver<Object>) arguments[index];
    }

    public void sendMessage(Object message) {
        final byte[] data = requestPack.pack(message);

        final byte[] compress = compressor.compress(data);

        stream.sendMessage(compress,false);

    }

    public void closeLocal() {

    }

    public void start(Listener responseListener, RpcInvocation invocation) {

    }

    public void cancel(GrpcStatus status, Throwable t) {

    }

    interface Listener {

        void onHeaders(Map<String, Object> attachments);

        void onMessage(Object message);

        void onClose(GrpcStatus status, Map<String, Object> trailers);
    }
}
