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

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.HttpMetadata;
import org.apache.dubbo.remoting.http12.HttpOutputMessage;
import org.apache.dubbo.remoting.http12.HttpVersion;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.remoting.http12.h2.Http2Header;
import org.apache.dubbo.remoting.http12.h2.Http2OutputMessage;
import org.apache.dubbo.remoting.http12.h2.Http2OutputMessageFrame;

import javax.servlet.AsyncContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_IO_EXCEPTION;

final class ServletStreamChannel implements H2StreamChannel {

    private static final ErrorTypeAwareLogger LOG = LoggerFactory.getErrorTypeAwareLogger(ServletStreamChannel.class);

    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final AsyncContext context;

    ServletStreamChannel(HttpServletRequest request, HttpServletResponse response, AsyncContext context) {
        this.request = request;
        this.response = response;
        this.context = context;
    }

    @Override
    public CompletableFuture<Void> writeResetFrame(long errorCode) {
        try {
            if (errorCode == 0L) {
                response.getOutputStream().close();
            } else if (errorCode >= 300 && errorCode < 600) {
                response.sendError((int) errorCode);
            } else {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } catch (Throwable t) {
            LOG.error(COMMON_IO_EXCEPTION, "", "", "Failed to close response", t);
        } finally {
            context.complete();
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Http2OutputMessage newOutputMessage(boolean endStream) {
        return new Http2OutputMessageFrame(new ByteArrayOutputStream(256), endStream);
    }

    @Override
    public CompletableFuture<Void> writeHeader(HttpMetadata httpMetadata) {
        boolean endStream = ((Http2Header) httpMetadata).isEndStream();
        try {
            HttpHeaders headers = httpMetadata.headers();
            if (endStream) {
                response.setTrailerFields(() -> {
                    Map<String, String> map = new HashMap<>();
                    for (Entry<String, List<String>> entry : headers.entrySet()) {
                        map.put(entry.getKey(), entry.getValue().get(0));
                    }
                    return map;
                });
            } else {
                for (Entry<String, List<String>> entry : headers.entrySet()) {
                    String key = entry.getKey();
                    List<String> values = entry.getValue();
                    if (HttpHeaderNames.STATUS.getName().equals(key)) {
                        response.setStatus(Integer.parseInt(values.get(0)));
                        continue;
                    }
                    if (values.size() == 1) {
                        response.setHeader(key, values.get(0));
                    } else {
                        for (int i = 0, size = values.size(); i < size; i++) {
                            response.addHeader(key, values.get(i));
                        }
                    }
                }
            }
        } catch (Throwable t) {
            LOG.error(COMMON_IO_EXCEPTION, "", "", "Failed to write header", t);
        } finally {
            if (endStream) {
                context.complete();
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> writeMessage(HttpOutputMessage httpOutputMessage) {
        boolean endStream = ((Http2OutputMessage) httpOutputMessage).isEndStream();
        try {
            ByteArrayOutputStream bos = (ByteArrayOutputStream) httpOutputMessage.getBody();
            ServletOutputStream out = response.getOutputStream();
            if (!HttpVersion.HTTP2.getProtocol().equals(request.getProtocol())) {
                response.setContentLength(bos.size());
            }
            bos.writeTo(out);
            out.flush();
        } catch (Throwable t) {
            LOG.error(COMMON_IO_EXCEPTION, "", "", "Failed to write message", t);
        } finally {
            if (endStream) {
                context.complete();
            }
        }
        return CompletableFuture.completedFuture(null);
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
}
