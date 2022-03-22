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
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.protocol.tri.ClassLoadUtil;
import org.apache.dubbo.rpc.protocol.tri.TripleConstant;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;
import org.apache.dubbo.rpc.protocol.tri.compressor.Compressor;
import org.apache.dubbo.rpc.protocol.tri.compressor.Identity;
import org.apache.dubbo.rpc.protocol.tri.observer.ServerCallToObserverAdapter;
import org.apache.dubbo.rpc.protocol.tri.stream.ServerStream;
import org.apache.dubbo.rpc.protocol.tri.stream.ServerStreamListener;
import org.apache.dubbo.rpc.protocol.tri.stream.StreamUtils;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

public abstract class ServerCall {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerCall.class);

    public final Invoker<?> invoker;
    public final FrameworkModel frameworkModel;
    public final ServerStream serverStream;
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

    ServerCall(Invoker<?> invoker,
        ServerStream serverStream,
        FrameworkModel frameworkModel,
        ServiceDescriptor serviceDescriptor,
        String acceptEncoding,
        String serviceName,
        String methodName,
        Executor executor) {
        this.invoker = invoker;
        this.executor = new SerializingExecutor(executor);
        this.frameworkModel = frameworkModel;
        this.serviceDescriptor = serviceDescriptor;
        this.serviceName = serviceName;
        this.methodName = methodName;
        this.serverStream = serverStream;
        this.acceptEncoding = acceptEncoding;
    }

    protected abstract ServerStreamListener doStartCall(Map<String, Object> metadata);

    /**
     * Build the RpcInvocation with metadata and execute headerFilter
     *
     * @param headers request header
     * @return RpcInvocation
     */
    protected RpcInvocation buildInvocation(Map<String, Object> headers,
        MethodDescriptor methodDescriptor) {
        final URL url = invoker.getUrl();
        RpcInvocation inv = new RpcInvocation(url.getServiceModel(),
            methodDescriptor.getMethodName(),
            serviceDescriptor.getInterfaceName(), url.getProtocolServiceKey(),
            methodDescriptor.getParameterClasses(),
            new Object[0]);
        inv.setTargetServiceUniqueName(url.getServiceKey());
        inv.setReturnTypes(methodDescriptor.getReturnTypes());
        inv.setObjectAttachments(StreamUtils.toAttachments(headers));
        return inv;
    }

    public ServerStreamListener startCall(Map<String, Object> metadata) {
        if (serviceDescriptor == null) {
            responseErr(
                TriRpcStatus.UNIMPLEMENTED.withDescription("Service not found:" + serviceName));
            return null;
        }

        // handle timeout
        String timeout = (String) metadata.get(TripleHeaderEnum.TIMEOUT.getHeader());
        try {
            if (Objects.nonNull(timeout)) {
                this.timeout = parseTimeoutToMills(timeout);
            }
        } catch (Throwable t) {
            LOGGER.warn(String.format("Failed to parse request timeout set from:%s, service=%s "
                + "method=%s", timeout, serviceDescriptor.getInterfaceName(), methodName));
        }
        return doStartCall(metadata);
    }

    private void sendHeader() {
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
        serverStream.sendHeader(headers);
    }

    public void requestN(int n) {
        serverStream.requestN(n);
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

    protected abstract byte[] packResponse(Object message) throws IOException;

    public void writeMessage(Object message) {
        final Runnable writeMessage = () -> doWriteMessage(message);
        executor.execute(writeMessage);
    }

    private void doWriteMessage(Object message) {
        if (closed) {
            return;
        }
        if (!headerSent) {
            sendHeader();
        }
        final byte[] data;
        try {
            data = packResponse(message);
        } catch (IOException e) {
            close(TriRpcStatus.INTERNAL.withDescription("Serialize response failed")
                .withCause(e), null);
            return;
        }
        if (data == null) {
            close(TriRpcStatus.INTERNAL.withDescription("Missing response"), null);
            return;
        }
        if (compressor != null) {
            int compressedFlag =
                Identity.MESSAGE_ENCODING.equals(compressor.getMessageEncoding()) ? 0 : 1;
            final byte[] compressed = compressor.compress(data);
            serverStream.writeMessage(compressed, compressedFlag);
        } else {
            serverStream.writeMessage(data, 0);
        }
    }

    public void close(TriRpcStatus status, Map<String, Object> trailers) {
        executor.execute(() -> serverStream.close(status, trailers));
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
        Http2Headers trailers = new DefaultHttp2Headers().status(OK.codeAsText())
            .set(HttpHeaderNames.CONTENT_TYPE, TripleConstant.CONTENT_PROTO)
            .setInt(TripleHeaderEnum.STATUS_KEY.getHeader(), status.code.code)
            .set(TripleHeaderEnum.MESSAGE_KEY.getHeader(), status.toEncodedMessage());
        serverStream.sendHeaderWithEos(trailers);
        LOGGER.error("Triple request error: service=" + serviceName + " method" + methodName,
            status.asException());
    }

    interface Listener {

        void onMessage(Object message);

        void onCancel(String errorInfo);

        void onComplete();
    }

    abstract class ServerStreamListenerBase implements ServerStreamListener {

        protected boolean closed;

        @Override
        public void onMessage(byte[] message) {
            if (closed) {
                return;
            }
            ClassLoader tccl = Thread.currentThread()
                .getContextClassLoader();
            try {
                doOnMessage(message);
            } catch (Throwable t) {
                final TriRpcStatus status = TriRpcStatus.INTERNAL.withDescription("Server error")
                    .withCause(t);
                close(status, null);
                LOGGER.error(
                    "Process request failed. service=" + serviceName + " method=" + methodName, t);
            } finally {
                ClassLoadUtil.switchContextLoader(tccl);
            }
        }

        protected abstract void doOnMessage(byte[] message)
            throws IOException, ClassNotFoundException;

    }

    public ServerCall.Listener startCall(
        RpcInvocation invocation,
        MethodDescriptor methodDescriptor,
        Invoker<?> invoker) {
        CancellationContext cancellationContext = RpcContext.getCancellationContext();
        ServerCallToObserverAdapter<Object> responseObserver =
            new ServerCallToObserverAdapter<>(this, cancellationContext);
        try {
            ServerCall.Listener listener;
            switch (methodDescriptor.getRpcType()) {
                case UNARY:
                    listener = new UnaryServerCallListener(invocation, invoker, responseObserver);
                    requestN(2);
                    break;
                case SERVER_STREAM:
                    listener = new ServerStreamServerCallListener(invocation, invoker,
                        responseObserver);
                    requestN(2);
                    break;
                case BI_STREAM:
                case CLIENT_STREAM:
                    listener = new BiStreamServerCallListener(invocation, invoker,
                        responseObserver);
                    requestN(1);
                    break;
                default:
                    throw new IllegalStateException("Can not reach here");
            }
            return listener;
        } catch (Throwable t) {
            LOGGER.error("Create triple stream failed", t);
            responseObserver.onError(TriRpcStatus.INTERNAL.withDescription("Create stream failed")
                .withCause(t)
                .asException());
        }
        return null;
    }
}
