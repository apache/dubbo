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

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.remoting.api.Connection;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.RequestMetadata;
import org.apache.dubbo.rpc.protocol.tri.compressor.Compressor;
import org.apache.dubbo.rpc.protocol.tri.compressor.Identity;
import org.apache.dubbo.rpc.protocol.tri.observer.ClientCallToObserverAdapter;
import org.apache.dubbo.rpc.protocol.tri.stream.ClientStream;
import org.apache.dubbo.rpc.protocol.tri.stream.StreamUtils;
import org.apache.dubbo.rpc.protocol.tri.stream.TripleClientStream;

import java.util.Map;
import java.util.concurrent.Executor;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_FAILED_RESPONSE;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_FAILED_SERIALIZE_TRIPLE;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_STREAM_LISTENER;

public class TripleClientCall implements ClientCall, ClientStream.Listener {

    private static final ErrorTypeAwareLogger LOGGER = LoggerFactory.getErrorTypeAwareLogger(TripleClientCall.class);
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
            LOGGER.warn(PROTOCOL_STREAM_LISTENER, "", "",
                "Received message from closed stream,connection=" + connection + " service="
                    + requestMetadata.service + " method="
                    + requestMetadata.method.getMethodName());
            return;
        }
        try {
            final Object unpacked = requestMetadata.packableMethod.parseResponse(message);
            listener.onMessage(unpacked);
        } catch (Throwable t) {
            TriRpcStatus status = TriRpcStatus.INTERNAL.withDescription("Deserialize response failed")
                .withCause(t);
            cancelByLocal(status.asException());
            listener.onClose(status,null);
            LOGGER.error(PROTOCOL_FAILED_RESPONSE, "", "", String.format("Failed to deserialize triple response, service=%s, method=%s,connection=%s",
                connection, requestMetadata.service, requestMetadata.method.getMethodName()), t);
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
        try {
            listener.onClose(status, StreamUtils.toAttachments(attachments));
        } catch (Throwable t) {
            cancelByLocal(
                TriRpcStatus.INTERNAL.withDescription("Close stream error").withCause(t)
                    .asException());
        }
        if (requestMetadata.cancellationContext != null) {
            requestMetadata.cancellationContext.cancel(null);
        }
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
            LOGGER.error(PROTOCOL_FAILED_SERIALIZE_TRIPLE, "", "", String.format("Serialize triple request failed, service=%s method=%s",
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
