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
import org.apache.dubbo.remoting.api.connection.AbstractConnectionClient;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.ByteBufPackableMethod;
import org.apache.dubbo.rpc.protocol.tri.RequestMetadata;
import org.apache.dubbo.rpc.protocol.tri.compressor.Compressor;
import org.apache.dubbo.rpc.protocol.tri.compressor.Identity;
import org.apache.dubbo.rpc.protocol.tri.observer.ClientCallToObserverAdapter;
import org.apache.dubbo.rpc.protocol.tri.stream.ClientStream;
import org.apache.dubbo.rpc.protocol.tri.stream.ClientStream.Listener;
import org.apache.dubbo.rpc.protocol.tri.stream.ClientStreamFactory;
import org.apache.dubbo.rpc.protocol.tri.stream.StreamUtils;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleWriteQueue;

import java.util.Map;
import java.util.concurrent.Executor;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.http2.Http2Exception.StreamException;

import static io.netty.handler.codec.http2.Http2Error.FLOW_CONTROL_ERROR;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_FAILED_RESPONSE;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_FAILED_SERIALIZE_TRIPLE;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_STREAM_LISTENER;

public class TripleClientCall implements ClientCall, Listener {
    private static final ErrorTypeAwareLogger LOGGER = LoggerFactory.getErrorTypeAwareLogger(TripleClientCall.class);
    private final AbstractConnectionClient connectionClient;
    private final Executor executor;
    private final FrameworkModel frameworkModel;
    private final TripleWriteQueue writeQueue;
    private RequestMetadata requestMetadata;
    private ClientStream stream;
    private Listener listener;
    private boolean canceled;
    private boolean headerSent;
    private boolean autoRequest = true;
    private boolean done;
    private StreamException streamException;

    public TripleClientCall(
            AbstractConnectionClient connectionClient,
            Executor executor,
            FrameworkModel frameworkModel,
            TripleWriteQueue writeQueue) {
        this.connectionClient = connectionClient;
        this.executor = executor;
        this.frameworkModel = frameworkModel;
        this.writeQueue = writeQueue;
    }

    // stream listener start
    @Override
    public void onMessage(ByteBuf message, boolean isReturnTriException) {
        if (done) {
            LOGGER.warn(
                    PROTOCOL_STREAM_LISTENER,
                    "",
                    "",
                    "Received message from closed stream,connection=" + connectionClient + " service="
                            + requestMetadata.service + " method="
                            + requestMetadata.method.getMethodName());
            return;
        }
        try {
            int contentLength = message.readableBytes();
            Object unpacked;
            if (requestMetadata.packableMethod instanceof ByteBufPackableMethod) {
                unpacked = ((ByteBufPackableMethod) requestMetadata.packableMethod)
                        .parseResponse(message, isReturnTriException);
            } else {
                byte[] data = new byte[contentLength];
                message.readBytes(data);
                unpacked = requestMetadata.packableMethod.parseResponse(data, isReturnTriException);
            }
            listener.onMessage(unpacked, contentLength);
        } catch (Throwable t) {
            TriRpcStatus status = TriRpcStatus.INTERNAL
                    .withDescription("Deserialize response failed")
                    .withCause(t);
            cancelByLocal(status.asException());
            listener.onClose(status, null, false);
            LOGGER.error(
                    PROTOCOL_FAILED_RESPONSE,
                    "",
                    "",
                    String.format(
                            "Failed to deserialize triple response, service=%s, method=%s,connection=%s",
                            requestMetadata.service, requestMetadata.service, requestMetadata.method.getMethodName()),
                    t);
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
        onComplete(status, null, null, false);
    }

    @Override
    public void onComplete(
            TriRpcStatus status,
            Map<String, Object> attachments,
            Map<CharSequence, String> excludeHeaders,
            boolean isReturnTriException) {
        if (done) {
            return;
        }
        done = true;
        try {
            listener.onClose(status, StreamUtils.toAttachments(attachments), isReturnTriException);
        } catch (Throwable t) {
            cancelByLocal(TriRpcStatus.INTERNAL
                    .withDescription("Close stream error")
                    .withCause(t)
                    .asException());
        }
        if (requestMetadata.cancellationContext != null) {
            requestMetadata.cancellationContext.cancel(null);
        }
    }

    @Override
    public void onClose() {
        if (done) {
            return;
        }
        onCancelByRemote(TriRpcStatus.CANCELLED);
    }

    @Override
    public void onStart() {
        listener.onStart(this);
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
        if (t instanceof StreamException && ((StreamException) t).error().equals(FLOW_CONTROL_ERROR)) {
            TriRpcStatus status = TriRpcStatus.CANCELLED
                    .withCause(t)
                    .withDescription("Due flowcontrol over pendingbytes, Cancelled by client");
            stream.cancelByLocal(status);
            streamException = (StreamException) t;
        } else {
            TriRpcStatus status = TriRpcStatus.CANCELLED.withCause(t).withDescription("Cancelled by client");
            stream.cancelByLocal(status);
        }
        TriRpcStatus status = TriRpcStatus.CANCELLED.withCause(t).withDescription("Cancelled by client");
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
        if (canceled && null != streamException) {
            throw new IllegalStateException("Due flowcontrol over pendingbytes, Call already canceled");
        } else if (canceled) {
            throw new IllegalStateException("Call already canceled");
        }
        if (!headerSent) {
            headerSent = true;
            stream.sendHeader(requestMetadata.toHeaders());
        }
        ByteBuf buffer = allocate();
        try {
            ByteBuf messageBuffer = prepareMessageBuffer(buffer, message);
            stream.sendMessage(messageBuffer).addListener(f -> {
                if (!f.isSuccess()) {
                    cancelByLocal(f.cause());
                }
            });
        } catch (Throwable t) {
            buffer.release();
            LOGGER.error(
                    PROTOCOL_FAILED_SERIALIZE_TRIPLE,
                    "",
                    "",
                    String.format(
                            "Serialize triple request failed, service=%s method=%s",
                            requestMetadata.service, requestMetadata.method.getMethodName()),
                    t);
            cancelByLocal(t);
            listener.onClose(
                    TriRpcStatus.INTERNAL
                            .withDescription("Serialize request failed")
                            .withCause(t),
                    null,
                    false);
        }
    }
    // stream listener end

    private ByteBuf prepareMessageBuffer(ByteBuf buffer, Object message) throws Exception {
        boolean isIdentityEncoding = Identity.MESSAGE_ENCODING.equals(requestMetadata.compressor.getMessageEncoding());
        ByteBuf targetBuffer = isIdentityEncoding ? buffer : allocate();

        targetBuffer.writeByte(isIdentityEncoding ? 0 : 1);
        int position = targetBuffer.writerIndex();
        targetBuffer.writerIndex(position + 4);

        packRequest(buffer, message);

        if (!isIdentityEncoding) {
            requestMetadata.compressor.compress(buffer, targetBuffer);
        }
        int len = targetBuffer.writerIndex() - position - 4;
        targetBuffer.setInt(position, len);
        return targetBuffer;
    }

    private void packRequest(ByteBuf buffer, Object message) throws Exception {
        if (requestMetadata.packableMethod instanceof ByteBufPackableMethod) {
            ((ByteBufPackableMethod) requestMetadata.packableMethod).packRequest(buffer, message);
        } else {
            byte[] data = requestMetadata.packableMethod.packRequest(message);
            buffer.writeBytes(data);
        }
    }

    @Override
    public void halfClose() {
        if (!headerSent) {
            return;
        }
        if (canceled) {
            return;
        }
        stream.halfClose().addListener(f -> {
            if (!f.isSuccess()) {
                cancelByLocal(new IllegalStateException("Half close failed", f.cause()));
            }
        });
    }

    @Override
    public void setCompression(String compression) {
        requestMetadata.compressor = Compressor.getCompressor(frameworkModel, compression);
    }

    @Override
    public StreamObserver<Object> start(RequestMetadata metadata, Listener responseListener) {
        ClientStream stream;
        for (ClientStreamFactory factory : frameworkModel.getActivateExtensions(ClientStreamFactory.class)) {
            stream = factory.createClientStream(connectionClient, frameworkModel, executor, this, writeQueue);
            if (stream != null) {
                this.requestMetadata = metadata;
                this.listener = responseListener;
                this.stream = stream;
                return new ClientCallToObserverAdapter<>(this);
            }
        }
        throw new IllegalStateException("No available ClientStreamFactory");
    }

    @Override
    public boolean isAutoRequest() {
        return autoRequest;
    }

    @Override
    public void setAutoRequest(boolean autoRequest) {
        this.autoRequest = autoRequest;
    }

    private ByteBuf allocate() {
        return this.connectionClient.<Channel>getChannel(true).alloc().buffer();
    }
}
