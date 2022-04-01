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
import org.apache.dubbo.rpc.protocol.tri.ClassLoadUtil;
import org.apache.dubbo.rpc.protocol.tri.ExceptionUtils;
import org.apache.dubbo.rpc.protocol.tri.RequestMetadata;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;
import org.apache.dubbo.rpc.protocol.tri.compressor.Compressor;
import org.apache.dubbo.rpc.protocol.tri.compressor.Identity;
import org.apache.dubbo.rpc.protocol.tri.observer.ClientCallToObserverAdapter;
import org.apache.dubbo.rpc.protocol.tri.stream.ClientStream;
import org.apache.dubbo.rpc.protocol.tri.stream.StreamUtils;
import org.apache.dubbo.rpc.protocol.tri.stream.TripleClientStream;

import com.google.protobuf.Any;
import com.google.rpc.DebugInfo;
import com.google.rpc.ErrorInfo;
import com.google.rpc.Status;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class TripleClientCall implements ClientCall, ClientStream.Listener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TripleClientCall.class);
    private final Connection connection;
    private final Executor executor;
    private final FrameworkModel frameworkModel;
    private RequestMetadata requestMetadata;
    private ClientStream stream;
    private ClientCall.Listener listener;
    private boolean canceled;
    private boolean headerSent;
    private boolean autoRequest = true;
    private boolean done;

    public TripleClientCall(Connection connection, Executor executor,
        FrameworkModel frameworkModel) {
        this.connection = connection;
        this.executor = executor;
        this.frameworkModel = frameworkModel;
    }


    // stream listener start
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
            final Object unpacked = requestMetadata.packableMethod.parseResponse(message);
            listener.onMessage(unpacked);
        } catch (IOException | ClassNotFoundException e) {
            cancelByLocal(TriRpcStatus.INTERNAL.withDescription("Deserialize response failed")
                .withCause(e).asException());
        }
    }

    @Override
    public void onCancelByRemote(TriRpcStatus status) {
        if (canceled) {
            return;
        }
        canceled = true;
        if (requestMetadata.cancellationContext != null) {
            requestMetadata.cancellationContext.cancel(status.asException());
        }
        onComplete(status, null, null);
    }

    @Override
    public void onComplete(TriRpcStatus status, Map<String, Object> attachments,
        Map<String, String> excludeHeaders) {
        if (done) {
            return;
        }
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
            cancelByLocal(
                TriRpcStatus.INTERNAL.withDescription("Close stream error").withCause(t)
                    .asException());
        }
        if (requestMetadata.cancellationContext != null) {
            requestMetadata.cancellationContext.cancel(null);
        }
    }

    private TriRpcStatus getStatusFromTrailers(Map<String, String> metadata) {
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

    @Override
    public void onStart() {
        listener.onStart(TripleClientCall.this);
    }

    @Override
    public void cancelByLocal(Throwable t) {
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
        TriRpcStatus status = TriRpcStatus.CANCELLED.withCause(t)
            .withDescription("Cancelled by client");
        stream.cancelByLocal(status);
        if (requestMetadata.cancellationContext != null) {
            requestMetadata.cancellationContext.cancel(t);
        }
    }

    @Override
    public void request(int messageNumber) {
        stream.request(messageNumber);
    }

    @Override
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
            stream.sendMessage(compress, compressed, false)
                .addListener(f -> {
                    if (!f.isSuccess()) {
                        cancelByLocal(f.cause());
                    }
                });
        } catch (Throwable t) {
            LOGGER.error(String.format("Serialize triple request failed, service=%s method=%s",
                requestMetadata.service,
                requestMetadata.method), t);
            cancelByLocal(t);
            listener.onClose(TriRpcStatus.INTERNAL.withDescription("Serialize request failed")
                .withCause(t), null);
        }
    }
    // stream listener end

    @Override
    public void halfClose() {
        if (!headerSent) {
            return;
        }
        if (canceled) {
            return;
        }
        stream.halfClose()
            .addListener(f -> {
                if (!f.isSuccess()) {
                    cancelByLocal(new IllegalStateException("Half close failed", f.cause()));
                }
            });
    }

    @Override
    public void setCompression(String compression) {
        this.requestMetadata.compressor = Compressor.getCompressor(frameworkModel, compression);
    }

    @Override
    public StreamObserver<Object> start(RequestMetadata metadata,
        ClientCall.Listener responseListener) {
        this.requestMetadata = metadata;
        this.listener = responseListener;
        this.stream = new TripleClientStream(frameworkModel, executor, connection.getChannel(),
            this);
        return new ClientCallToObserverAdapter<>(this);
    }

    @Override
    public boolean isAutoRequest() {
        return autoRequest;
    }

    @Override
    public void setAutoRequest(boolean autoRequest) {
        this.autoRequest = autoRequest;
    }

}
