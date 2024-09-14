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
package org.apache.dubbo.rpc.protocol.tri.servlet;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.http12.HttpConstants;
import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.HttpMetadata;
import org.apache.dubbo.remoting.http12.HttpOutputMessage;
import org.apache.dubbo.remoting.http12.exception.HttpStatusException;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.remoting.http12.h2.Http2Header;
import org.apache.dubbo.remoting.http12.h2.Http2OutputMessage;
import org.apache.dubbo.remoting.http12.h2.Http2OutputMessageFrame;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.TriRpcStatus.Code;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;

import javax.servlet.AsyncContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

final class ServletStreamChannel implements H2StreamChannel {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServletStreamChannel.class);

    private final Queue<Object> writeQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean writeable = new AtomicBoolean();
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final AsyncContext context;

    private boolean isGrpc;

    ServletStreamChannel(HttpServletRequest request, HttpServletResponse response, AsyncContext context) {
        this.request = request;
        this.response = response;
        this.context = context;
    }

    public void setGrpc(boolean isGrpc) {
        this.isGrpc = isGrpc;
    }

    public void writeError(int code, Throwable throwable) {
        if (response.isCommitted() && code == Code.DEADLINE_EXCEEDED.code) {
            return;
        }
        try {
            if (isGrpc) {
                response.setTrailerFields(() -> {
                    Map<String, String> map = new HashMap<>();
                    map.put(TripleHeaderEnum.STATUS_KEY.getName(), String.valueOf(code));
                    return map;
                });
                return;
            }
            try {
                if (throwable instanceof HttpStatusException) {
                    response.setStatus(((HttpStatusException) throwable).getStatusCode());
                    response.getOutputStream().close();
                } else {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            } catch (Throwable t) {
                LOGGER.info("Failed to send response", t);
            }
        } finally {
            context.complete();
        }
    }

    public void onWritePossible() {
        if (writeable.compareAndSet(false, true)) {
            flushQueue();
        }
    }

    private void flushQueue() {
        if (writeQueue.isEmpty()) {
            return;
        }
        synchronized (writeQueue) {
            Object obj;
            while ((obj = writeQueue.poll()) != null) {
                if (obj instanceof HttpMetadata) {
                    writeHeaderInternal((HttpMetadata) obj);
                } else if (obj instanceof HttpOutputMessage) {
                    writeMessageInternal((HttpOutputMessage) obj);
                }
            }
        }
    }

    @Override
    public CompletableFuture<Void> writeResetFrame(long errorCode) {
        if (isGrpc) {
            writeError(TriRpcStatus.httpStatusToGrpcCode((int) errorCode).code, null);
            return completed();
        }

        try {
            if (errorCode == 0L) {
                response.getOutputStream().close();
                return completed();
            }
            if (response.isCommitted()) {
                return completed();
            }
            if (errorCode >= 300 && errorCode < 600) {
                response.sendError((int) errorCode);
            } else {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } catch (Throwable t) {
            LOGGER.info("Failed to close response", t);
        } finally {
            context.complete();
        }

        return completed();
    }

    @Override
    public Http2OutputMessage newOutputMessage(boolean endStream) {
        return new Http2OutputMessageFrame(new ByteArrayOutputStream(256), endStream);
    }

    @Override
    public CompletableFuture<Void> writeHeader(HttpMetadata httpMetadata) {
        if (writeable.get()) {
            flushQueue();
            writeHeaderInternal(httpMetadata);
        } else {
            writeQueue.add(httpMetadata);
        }
        return completed();
    }

    private void writeHeaderInternal(HttpMetadata httpMetadata) {
        boolean endStream = false;
        boolean isHttp1 = true;
        if (httpMetadata instanceof Http2Header) {
            endStream = ((Http2Header) httpMetadata).isEndStream();
            isHttp1 = false;
        }
        try {
            HttpHeaders headers = httpMetadata.headers();
            if (endStream) {
                response.setTrailerFields(() -> {
                    Map<String, String> map = new HashMap<>();
                    for (Entry<CharSequence, String> entry : headers) {
                        map.put(entry.getKey().toString(), entry.getValue());
                    }
                    return map;
                });
                return;
            }

            if (response.isCommitted()) {
                return;
            }

            for (Entry<CharSequence, String> entry : headers) {
                String key = entry.getKey().toString();
                String value = entry.getValue();
                if (HttpHeaderNames.STATUS.getName().equals(key)) {
                    response.setStatus(Integer.parseInt(value));
                    continue;
                }
                if (isHttp1
                        && HttpHeaderNames.TRANSFER_ENCODING.getName().equals(key)
                        && HttpConstants.CHUNKED.equals(value)) {
                    continue;
                }
                response.addHeader(key, value);
            }
        } catch (Throwable t) {
            LOGGER.info("Failed to write header", t);
        } finally {
            if (endStream) {
                context.complete();
            }
        }
    }

    @Override
    public CompletableFuture<Void> writeMessage(HttpOutputMessage httpOutputMessage) {
        if (writeable.get()) {
            flushQueue();
            writeMessageInternal(httpOutputMessage);
        } else {
            writeQueue.add(httpOutputMessage);
        }
        return completed();
    }

    private void writeMessageInternal(HttpOutputMessage httpOutputMessage) {
        boolean endStream = false;
        if (httpOutputMessage instanceof Http2OutputMessage) {
            endStream = ((Http2OutputMessage) httpOutputMessage).isEndStream();
        } else if (httpOutputMessage == HttpOutputMessage.EMPTY_MESSAGE) {
            endStream = true;
        }
        try {
            ByteArrayOutputStream bos = (ByteArrayOutputStream) httpOutputMessage.getBody();
            ServletOutputStream out = response.getOutputStream();
            bos.writeTo(out);
            out.flush();
        } catch (Throwable t) {
            LOGGER.info("Failed to write message", t);
        } finally {
            if (endStream) {
                context.complete();
            }
        }
    }

    @Override
    public SocketAddress remoteAddress() {
        return InetSocketAddress.createUnresolved(request.getRemoteAddr(), request.getRemotePort());
    }

    @Override
    public SocketAddress localAddress() {
        return InetSocketAddress.createUnresolved(request.getLocalAddr(), request.getLocalPort());
    }

    @Override
    public void flush() {}

    private static CompletableFuture<Void> completed() {
        return CompletableFuture.completedFuture(null);
    }
}
