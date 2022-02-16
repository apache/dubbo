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
import org.apache.dubbo.remoting.api.Connection;
import org.apache.dubbo.rpc.protocol.tri.ClassLoadUtil;
import org.apache.dubbo.rpc.protocol.tri.ExceptionUtils;
import org.apache.dubbo.rpc.protocol.tri.RequestMetadata;
import org.apache.dubbo.rpc.protocol.tri.RpcStatus;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;
import org.apache.dubbo.rpc.protocol.tri.compressor.Compressor;
import org.apache.dubbo.rpc.protocol.tri.compressor.Identity;
import org.apache.dubbo.rpc.protocol.tri.observer.H2TransportListener;
import org.apache.dubbo.rpc.protocol.tri.pack.PbPack;
import org.apache.dubbo.rpc.protocol.tri.pack.PbUnpack;
import org.apache.dubbo.rpc.protocol.tri.stream.ClientStream;
import org.apache.dubbo.rpc.protocol.tri.stream.ClientStreamListener;

import com.google.protobuf.Any;
import com.google.rpc.DebugInfo;
import com.google.rpc.ErrorInfo;
import com.google.rpc.Status;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

public class ClientCall {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientCall.class);
    private final Connection connection;
    private final Executor executor;
    private final URL url;
    private RequestMetadata requestMetadata;
    private ClientStream stream;
    private boolean canceled;
    private boolean headerSent;

    public ClientCall(URL url,
                      Connection connection,
                      ExecutorService executor
    ) {
        this.url = url;
        this.executor = executor;
        this.connection = connection;
    }

    public void sendMessage(Object message) {
        if (canceled) {
            throw new IllegalStateException("Call already canceled");
        }
        if (!headerSent) {
            headerSent = true;
            stream.startCall(requestMetadata);
        }
        final byte[] data;
        try {
            data = PbPack.INSTANCE.pack(message);
            int compressed = Identity.MESSAGE_ENCODING.equals(requestMetadata.compressor.getMessageEncoding()) ? 0 : 1;
            final byte[] compress = requestMetadata.compressor.compress(data);
            stream.writeMessage(compress, compressed);
        } catch (IOException e) {
            cancel("Serialize request failed", e);
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
        this.requestMetadata.compressor = Compressor.getCompressor(url.getOrDefaultFrameworkModel(), compression);
    }

    public void start(RequestMetadata metadata, ClientCall.StartListener responseListener) {
        this.requestMetadata = metadata;
        final PbUnpack<?> unpack = requestMetadata.method.isNeedWrap() ?
            PbUnpack.RESP_PB_UNPACK : new PbUnpack<>(requestMetadata.method.getReturnClass());

        this.stream = new ClientStream(
            url,
            metadata.requestId,
            executor,
            connection.getChannel(),
            new ClientStreamListenerImpl(responseListener, unpack));
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
        RpcStatus status = RpcStatus.CANCELLED.withCause(t);
        if (message != null) {
            status = status.withDescription(message);
        } else {
            status = status.withDescription("Cancel by client without message");
        }
        stream.cancelByLocal(status);
    }

    interface Listener {

        void onMessage(Object message);

        void onClose(RpcStatus status, Map<String, Object> trailers);
    }


    public interface StartListener extends Listener {

        void onStart();
    }

    class ClientStreamListenerImpl implements ClientStreamListener {

        private final StartListener listener;
        private final PbUnpack<?> unpack;
        private boolean done;

        ClientStreamListenerImpl(StartListener listener, PbUnpack<?> unpack) {
            this.unpack = unpack;
            this.listener = listener;
        }

        @Override
        public void onStart() {
            listener.onStart();
        }

        @Override
        public void onMessage(byte[] message) {
            if (done) {
                LOGGER.warn("Received message from closed stream,connection=" + connection
                    + " service=" + requestMetadata.service + " method=" + requestMetadata.method.getMethodName());
                return;
            }
            try {
                final Object unpacked = unpack.unpack(message);
                listener.onMessage(unpacked);
            } catch (IOException e) {
                cancelByErr(RpcStatus.INTERNAL
                    .withDescription("Deserialize response failed")
                    .withCause(e));
            }
        }

        @Override
        public void complete(RpcStatus status, Map<String, Object> attachments, Map<String, String> excludeHeaders) {
            executor.execute(() -> {
                done = true;
                final RpcStatus detailStatus;
                final RpcStatus statusFromTrailers = getStatusFromTrailers(excludeHeaders);
                if (statusFromTrailers != null) {
                    detailStatus = statusFromTrailers;
                } else {
                    detailStatus = status;
                }
                try {
                    listener.onClose(detailStatus, attachments);
                } catch (Throwable t) {
                    cancelByErr(RpcStatus.INTERNAL
                        .withDescription("Close stream error")
                        .withCause(t));
                }
            });
        }

        void cancelByErr(RpcStatus status) {
            stream.cancelByLocal(status);
        }

        RpcStatus getStatusFromTrailers(Map<String, String> metadata) {
            if (null == metadata) {
                return null;
            }
            // second get status detail
            if (!metadata.containsKey(TripleHeaderEnum.STATUS_DETAIL_KEY.getHeader())) {
                return null;
            }
            final String raw = (metadata.remove(TripleHeaderEnum.STATUS_DETAIL_KEY.getHeader()));
            byte[] statusDetailBin = H2TransportListener.decodeASCIIByte(raw);
            ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            try {
                final Status statusDetail = Status.parseFrom(statusDetailBin);
                List<Any> detailList = statusDetail.getDetailsList();
                Map<Class<?>, Object> classObjectMap = tranFromStatusDetails(detailList);

                // get common exception from DebugInfo
                RpcStatus status = RpcStatus.fromCode(statusDetail.getCode())
                    .withDescription(RpcStatus.decodeMessage(statusDetail.getMessage()));
                DebugInfo debugInfo = (DebugInfo) classObjectMap.get(DebugInfo.class);
                if (debugInfo != null) {
                    String msg = ExceptionUtils.getStackFrameString(debugInfo.getStackEntriesList());
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
