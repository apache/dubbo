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
import org.apache.dubbo.common.threadpool.serial.SerializingExecutor;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.CancellationContext;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.PackableMethod;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.protocol.tri.ClassLoadUtil;
import org.apache.dubbo.rpc.protocol.tri.TripleConstant;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;
import org.apache.dubbo.rpc.protocol.tri.compressor.Compressor;
import org.apache.dubbo.rpc.protocol.tri.compressor.Identity;
import org.apache.dubbo.rpc.protocol.tri.observer.ServerCallToObserverAdapter;
import org.apache.dubbo.rpc.protocol.tri.stream.ServerStream;
import org.apache.dubbo.rpc.protocol.tri.stream.StreamUtils;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.util.concurrent.Future;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public abstract class AbstractServerCall implements ServerCall, ServerStream.Listener {

    public static final String REMOTE_ADDRESS_KEY = "tri.remote.address";
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractServerCall.class);

    public final Invoker<?> invoker;
    public final FrameworkModel frameworkModel;
    public final ServerStream stream;
    public final Executor executor;
    public final String methodName;
    public final String serviceName;
    public final ServiceDescriptor serviceDescriptor;
    private final String acceptEncoding;
    public boolean autoRequestN = true;
    public Long timeout;
    ServerCall.Listener listener;
    private Compressor compressor;
    private boolean headerSent;
    private boolean closed;
    CancellationContext cancellationContext;
    protected MethodDescriptor methodDescriptor;
    protected PackableMethod packableMethod;
    protected Map<String, Object> requestMetadata;

    AbstractServerCall(Invoker<?> invoker,
        ServerStream stream,
        FrameworkModel frameworkModel,
        ServiceDescriptor serviceDescriptor,
        String acceptEncoding,
        String serviceName,
        String methodName,
        Executor executor
    ) {
        Objects.requireNonNull(serviceDescriptor,
            "No service descriptor found for " + invoker.getUrl());
        this.invoker = invoker;
        this.executor = new SerializingExecutor(executor);
        this.frameworkModel = frameworkModel;
        this.serviceDescriptor = serviceDescriptor;
        this.serviceName = serviceName;
        this.methodName = methodName;
        this.stream = stream;
        this.acceptEncoding = acceptEncoding;
    }


    // stream listener start
    @Override
    public void onHeader(Map<String, Object> requestMetadata) {
        this.requestMetadata = requestMetadata;
        if (serviceDescriptor == null) {
            responseErr(
                TriRpcStatus.UNIMPLEMENTED.withDescription("Service not found:" + serviceName));
            return;
        }
        startCall();
    }

    protected void startCall() {
        RpcInvocation invocation = buildInvocation(methodDescriptor);
        listener = startInternalCall(invocation, methodDescriptor, invoker);
    }

    @Override
    public final void request(int numMessages) {
        stream.request(numMessages);
    }

    @Override
    public final void sendMessage(Object message) {
        if (closed) {
            throw new IllegalStateException("Stream has already canceled");
        }
        final Runnable sendMessage = () -> doSendMessage(message);
        executor.execute(sendMessage);
    }

    private void doSendMessage(Object message) {
        if (closed) {
            return;
        }
        if (!headerSent) {
            sendHeader();
        }
        final byte[] data;
        try {
            data = packableMethod.packResponse(message);
        } catch (IOException e) {
            close(TriRpcStatus.INTERNAL.withDescription("Serialize response failed")
                .withCause(e), null);
            return;
        }
        if (data == null) {
            close(TriRpcStatus.INTERNAL.withDescription("Missing response"), null);
            return;
        }
        Future<?> future;
        if (compressor != null) {
            int compressedFlag =
                Identity.MESSAGE_ENCODING.equals(compressor.getMessageEncoding()) ? 0 : 1;
            final byte[] compressed = compressor.compress(data);
            future = stream.sendMessage(compressed, compressedFlag);
        } else {
            future = stream.sendMessage(data, 0);
        }
        future.addListener(f -> {
            if (!f.isSuccess()) {
                cancelDual(TriRpcStatus.CANCELLED
                    .withDescription("Send message failed")
                    .withCause(f.cause()));
            }
        });
    }

    @Override
    public final void onComplete() {
        listener.onComplete();
    }

    @Override
    public final void onMessage(byte[] message) {
        ClassLoader tccl = Thread.currentThread()
            .getContextClassLoader();
        try {
            Object instance = parseSingleMessage(message);
            listener.onMessage(instance);
        } catch (Throwable t) {
            final TriRpcStatus status = TriRpcStatus.UNKNOWN.withDescription("Server error")
                .withCause(t);
            close(status, null);
            LOGGER.error("Process request failed. service=" + serviceName +
                " method=" + methodName, t);
        } finally {
            ClassLoadUtil.switchContextLoader(tccl);
        }
    }

    protected abstract Object parseSingleMessage(byte[] data)
        throws IOException, ClassNotFoundException;

    @Override
    public final void onCancelByRemote(TriRpcStatus status) {
        closed = true;
        cancellationContext.cancel(status.cause);
        listener.onCancel(status);
    }
    // stream listener end


    public final boolean isClosed() {
        return closed;
    }

    /**
     * Build the RpcInvocation with metadata and execute headerFilter
     *
     * @return RpcInvocation
     */
    protected RpcInvocation buildInvocation(MethodDescriptor methodDescriptor) {
        final URL url = invoker.getUrl();
        RpcInvocation inv = new RpcInvocation(url.getServiceModel(),
            methodDescriptor.getMethodName(),
            serviceDescriptor.getInterfaceName(), url.getProtocolServiceKey(),
            methodDescriptor.getParameterClasses(),
            new Object[0]);
        inv.setTargetServiceUniqueName(url.getServiceKey());
        inv.setReturnTypes(methodDescriptor.getReturnTypes());
        inv.setObjectAttachments(StreamUtils.toAttachments(requestMetadata));
        inv.put(REMOTE_ADDRESS_KEY, stream.remoteAddress());
        // handle timeout
        String timeout = (String) requestMetadata.get(TripleHeaderEnum.TIMEOUT.getHeader());
        try {
            if (Objects.nonNull(timeout)) {
                this.timeout = parseTimeoutToMills(timeout);
            }
        } catch (Throwable t) {
            LOGGER.warn(String.format("Failed to parse request timeout set from:%s, service=%s "
                + "method=%s", timeout, serviceDescriptor.getInterfaceName(), methodName));
        }
        if (null != requestMetadata.get(TripleHeaderEnum.CONSUMER_APP_NAME_KEY.getHeader())) {
            inv.put(TripleHeaderEnum.CONSUMER_APP_NAME_KEY,
                requestMetadata.get(TripleHeaderEnum.CONSUMER_APP_NAME_KEY.getHeader()));
        }
        return inv;
    }


    private void sendHeader() {
        if (closed) {
            return;
        }
        if (headerSent) {
            throw new IllegalStateException("Header has already sent");
        }
        headerSent = true;
        DefaultHttp2Headers headers = new DefaultHttp2Headers();
        headers.status(HttpResponseStatus.OK.codeAsText());
        headers.set(HttpHeaderNames.CONTENT_TYPE, TripleConstant.CONTENT_PROTO);
        if (acceptEncoding != null) {
            headers.set(HttpHeaderNames.ACCEPT_ENCODING, acceptEncoding);
        }
        if (compressor != null) {
            headers.set(TripleHeaderEnum.GRPC_ENCODING.getHeader(),
                compressor.getMessageEncoding());
        }
        // send header failed will reset stream and close request observer cause no more data will be sent
        stream.sendHeader(headers)
            .addListener(f -> {
                if (!f.isSuccess()) {
                    cancelDual(TriRpcStatus.INTERNAL.withCause(f.cause()));
                }
            });
    }

    private void cancelDual(TriRpcStatus status) {
        closed = true;
        listener.onCancel(status);
        cancellationContext.cancel(status.asException());
    }

    public void cancelByLocal(Throwable throwable) {
        if (closed) {
            return;
        }
        closed = true;
        cancellationContext.cancel(throwable);
        stream.cancelByLocal(TriRpcStatus.CANCELLED.withCause(throwable));
    }


    public void setCompression(String compression) {
        if (headerSent) {
            throw new IllegalStateException("Can not set compression after header sent");
        }
        this.compressor = Compressor.getCompressor(frameworkModel, compression);
    }

    public void disableAutoRequestN() {
        autoRequestN = false;
    }


    public boolean isAutoRequestN() {
        return autoRequestN;
    }


    public void close(TriRpcStatus status, Map<String, Object> attachments) {
        executor.execute(() -> doClose(status, attachments));
    }

    private void doClose(TriRpcStatus status, Map<String, Object> attachments) {
        if (closed) {
            return;
        }
        closed = true;
        stream.complete(status, attachments);
    }

    protected Long parseTimeoutToMills(String timeoutVal) {
        if (StringUtils.isEmpty(timeoutVal) || StringUtils.isContains(timeoutVal, "null")) {
            return null;
        }
        long value = Long.parseLong(timeoutVal.substring(0, timeoutVal.length() - 1));
        char unit = timeoutVal.charAt(timeoutVal.length() - 1);
        switch (unit) {
            case 'n':
                return TimeUnit.NANOSECONDS.toMillis(value);
            case 'u':
                return TimeUnit.MICROSECONDS.toMillis(value);
            case 'm':
                return value;
            case 'S':
                return TimeUnit.SECONDS.toMillis(value);
            case 'M':
                return TimeUnit.MINUTES.toMillis(value);
            case 'H':
                return TimeUnit.HOURS.toMillis(value);
            default:
                // invalid timeout config
                return null;
        }
    }

    /**
     * Error in create stream, unsupported config or triple protocol error.
     *
     * @param status response status
     */
    protected void responseErr(TriRpcStatus status) {
        if (closed) {
            return;
        }
        closed = true;
        stream.complete(status, null);
        LOGGER.error("Triple request error: service=" + serviceName + " method" + methodName,
            status.asException());
    }


    protected ServerCall.Listener startInternalCall(
        RpcInvocation invocation,
        MethodDescriptor methodDescriptor,
        Invoker<?> invoker) {
        this.cancellationContext = RpcContext.getCancellationContext();
        ServerCallToObserverAdapter<Object> responseObserver =
            new ServerCallToObserverAdapter<>(this, cancellationContext);
        try {
            ServerCall.Listener listener;
            switch (methodDescriptor.getRpcType()) {
                case UNARY:
                    listener = new UnaryServerCallListener(invocation, invoker, responseObserver);
                    request(2);
                    break;
                case SERVER_STREAM:
                    listener = new ServerStreamServerCallListener(invocation, invoker,
                        responseObserver);
                    request(2);
                    break;
                case BI_STREAM:
                case CLIENT_STREAM:
                    listener = new BiStreamServerCallListener(invocation, invoker,
                        responseObserver);
                    request(1);
                    break;
                default:
                    throw new IllegalStateException("Can not reach here");
            }
            return listener;
        } catch (Throwable t) {
            LOGGER.error("Create triple stream failed", t);
            responseErr(TriRpcStatus.INTERNAL.withDescription("Create stream failed")
                .withCause(t));
        }
        return null;
    }
}
