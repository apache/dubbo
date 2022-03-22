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

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.remoting.api.Connection;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.PackableMethod;
import org.apache.dubbo.rpc.protocol.tri.ClassLoadUtil;
import org.apache.dubbo.rpc.protocol.tri.ExceptionUtils;
import org.apache.dubbo.rpc.protocol.tri.RequestMetadata;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;
import org.apache.dubbo.rpc.protocol.tri.compressor.Compressor;
import org.apache.dubbo.rpc.protocol.tri.compressor.Identity;
import org.apache.dubbo.rpc.protocol.tri.observer.ClientCallToObserverAdapter;
import org.apache.dubbo.rpc.protocol.tri.stream.ClientStream;
import org.apache.dubbo.rpc.protocol.tri.stream.ClientStreamListener;
import org.apache.dubbo.rpc.protocol.tri.stream.StreamUtils;

import com.google.protobuf.Any;
import com.google.rpc.DebugInfo;
import com.google.rpc.ErrorInfo;
import com.google.rpc.Status;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class ClientCall {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientCall.class);
    private final Connection connection;
    private final Executor executor;
    private final FrameworkModel frameworkModel;
    private RequestMetadata requestMetadata;
    private ClientStream stream;
    private ClientCall.Listener listener;
    private boolean canceled;
    private boolean headerSent;
    private boolean autoRequestN = true;

    public ClientCall(Connection connection, Executor executor, FrameworkModel frameworkModel) {
        this.connection = connection;
        this.executor = executor;
        this.frameworkModel = frameworkModel;
    }

    public void sendMessage(Object message) {
        if (canceled) {
            throw new IllegalStateException("Call already canceled");
        }
        if (!headerSent) {
            headerSent = true;
            stream.sendHeader(requestMetadata.toHeaders());
        }
        final byte[] data;
        try {
            data = requestMetadata.packableMethod.packRequest(message);
            int compressed =
                Identity.MESSAGE_ENCODING.equals(requestMetadata.compressor.getMessageEncoding())
                    ? 0 : 1;
            final byte[] compress = requestMetadata.compressor.compress(data);
            stream.writeMessage(compress, compressed);
        } catch (Throwable t) {
            LOGGER.error(String.format("Serialize triple request failed, service=%s method=%s",
                requestMetadata.service,
                requestMetadata.method), t);
            cancel("Serialize request failed", t);
            listener.onClose(TriRpcStatus.INTERNAL.withDescription("Serialize request failed")
                .withCause(t), null);
        }
    }


    public void requestN(int n) {
        stream.requestN(n);
    }

    public void halfClose() {
        if (!headerSent) {
            return;
        }
        if (canceled) {
            return;
        }
        stream.halfClose();
    }

    public void setCompression(String compression) {
        this.requestMetadata.compressor = Compressor.getCompressor(frameworkModel, compression);
    }

    public StreamObserver<Object> start(RequestMetadata metadata,
        ClientCall.Listener responseListener) {
        this.requestMetadata = metadata;
        this.listener = responseListener;
        this.stream = new ClientStream(frameworkModel, executor, connection.getChannel(),
            new ClientStreamListenerImpl(responseListener, metadata.packableMethod));
        return new ClientCallToObserverAdapter<>(this);
    }

    public void cancel(String message, Throwable t) {
        if (canceled) {
            return;
        }
        // did not create stream
        if (!headerSent) {
            return;
        }
        canceled = true;
        if (stream == null) {
            return;
        }
        TriRpcStatus status = TriRpcStatus.CANCELLED.withCause(t);
        if (message != null) {
            status = status.withDescription(message);
        } else {
            status = status.withDescription("Cancel by client without message");
        }
        stream.cancelByLocal(status);
    }

    public boolean isAutoRequestN() {
        return autoRequestN;
    }

    public void setAutoRequestN(boolean autoRequestN) {
        this.autoRequestN = autoRequestN;
    }

    public interface Listener {

        void onStart(ClientCall call);

        void onMessage(Object message);

        void onClose(TriRpcStatus status, Map<String, Object> trailers);
    }

    class ClientStreamListenerImpl implements ClientStreamListener {

        private final Listener listener;
        private final PackableMethod packableMethod;
        private boolean done;

        ClientStreamListenerImpl(Listener listener, PackableMethod packableMethod) {
            this.listener = listener;
            this.packableMethod = packableMethod;
        }

        @Override
        public void onStart() {
            listener.onStart(ClientCall.this);
        }

        @Override
        public void onMessage(byte[] message) {
            if (done) {
                LOGGER.warn(
                    "Received message from closed stream,connection=" + connection + " service="
                        + requestMetadata.service + " method="
                        + requestMetadata.method.getMethodName());
                return;
            }
            try {
                final Object unpacked = packableMethod.parseResponse(message);
                listener.onMessage(unpacked);
            } catch (IOException | ClassNotFoundException e) {
                cancelByErr(TriRpcStatus.INTERNAL.withDescription("Deserialize response failed")
                    .withCause(e));
            }
        }

        @Override
        public void complete(TriRpcStatus status, Map<String, Object> attachments,
            Map<String, String> excludeHeaders) {
            done = true;
            final TriRpcStatus detailStatus;
            final TriRpcStatus statusFromTrailers = getStatusFromTrailers(excludeHeaders);
            if (statusFromTrailers != null) {
                detailStatus = statusFromTrailers;
            } else {
                detailStatus = status;
            }
            try {
                listener.onClose(detailStatus, StreamUtils.toAttachments(attachments));
            } catch (Throwable t) {
                cancelByErr(
                    TriRpcStatus.INTERNAL.withDescription("Close stream error").withCause(t));
            }
        }

        void cancelByErr(TriRpcStatus status) {
            stream.cancelByLocal(status);
        }

        TriRpcStatus getStatusFromTrailers(Map<String, String> metadata) {
            if (null == metadata) {
                return null;
            }
            // second get status detail
            if (!metadata.containsKey(TripleHeaderEnum.STATUS_DETAIL_KEY.getHeader())) {
                return null;
            }
            final String raw = (metadata.remove(TripleHeaderEnum.STATUS_DETAIL_KEY.getHeader()));
            byte[] statusDetailBin = StreamUtils.decodeASCIIByte(raw);
            ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            try {
                final Status statusDetail = Status.parseFrom(statusDetailBin);
                List<Any> detailList = statusDetail.getDetailsList();
                Map<Class<?>, Object> classObjectMap = tranFromStatusDetails(detailList);

                // get common exception from DebugInfo
                TriRpcStatus status = TriRpcStatus.fromCode(statusDetail.getCode())
                    .withDescription(TriRpcStatus.decodeMessage(statusDetail.getMessage()));
                DebugInfo debugInfo = (DebugInfo) classObjectMap.get(DebugInfo.class);
                if (debugInfo != null) {
                    String msg = ExceptionUtils.getStackFrameString(
                        debugInfo.getStackEntriesList());
                    status = status.appendDescription(msg);
                }
                return status;
            } catch (IOException ioException) {
                return null;
            } finally {
                ClassLoadUtil.switchContextLoader(tccl);
            }

        }

        private Map<Class<?>, Object> tranFromStatusDetails(List<Any> detailList) {
            Map<Class<?>, Object> map = new HashMap<>();
            try {
                for (Any any : detailList) {
                    if (any.is(ErrorInfo.class)) {
                        ErrorInfo errorInfo = any.unpack(ErrorInfo.class);
                        map.putIfAbsent(ErrorInfo.class, errorInfo);
                    } else if (any.is(DebugInfo.class)) {
                        DebugInfo debugInfo = any.unpack(DebugInfo.class);
                        map.putIfAbsent(DebugInfo.class, debugInfo);
                    }
                    // support others type but now only support this
                }
            } catch (Throwable t) {
                LOGGER.error("tran from grpc-status-details error", t);
            }
            return map;
        }
    }
}
