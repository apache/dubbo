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
package org.apache.dubbo.xds.security;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.remoting.http12.h2.Http2Header;
import org.apache.dubbo.remoting.http12.h2.Http2InputMessage;
import org.apache.dubbo.remoting.http12.h2.Http2MetadataFrame;
import org.apache.dubbo.remoting.http12.h2.Http2ServerTransportListenerDecorator;
import org.apache.dubbo.remoting.http12.h2.Http2TransportListener;
import org.apache.dubbo.remoting.http12.netty4.NettyHttpHeaders;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.net.InetSocketAddress;

import io.netty.handler.codec.http2.DefaultHttp2Headers;

/** Runs before every HTTP/2 listener, including internal services exported with filter=-default. */
@Activate(order = -10000)
public final class NativeServerTransportDecorator implements Http2ServerTransportListenerDecorator {
    @Override
    public Http2TransportListener decorate(
            Http2TransportListener listener, H2StreamChannel channel, URL url, FrameworkModel framework) {
        int port = channel.localAddress() instanceof InetSocketAddress
                ? ((InetSocketAddress) channel.localAddress()).getPort()
                : url.getPort();
        XdsServerBindings.Binding binding = XdsServerBindings.binding(port);
        if (binding == null) {
            return listener;
        }
        return new GuardedListener(listener, channel, binding);
    }

    private static final class GuardedListener implements Http2TransportListener {
        final Http2TransportListener delegate;
        final H2StreamChannel channel;
        final XdsServerBindings.Binding binding;
        boolean checked;
        boolean denied;

        GuardedListener(Http2TransportListener delegate, H2StreamChannel channel, XdsServerBindings.Binding binding) {
            this.delegate = delegate;
            this.channel = channel;
            this.binding = binding;
        }

        @Override
        public void onMetadata(Http2Header metadata) {
            if (!checked) {
                checked = true;
                InboundPolicy policy = binding.policy;
                try {
                    denied = binding.closed.get()
                            || policy == null
                            || !policy.permits(
                                    binding.runtime.getSecurity(),
                                    metadata.headers(),
                                    channel.localAddress(),
                                    channel.remoteAddress(),
                                    channel.sslSession());
                } catch (RuntimeException failure) {
                    denied = true;
                }
                if (denied) {
                    HttpHeaders headers = new NettyHttpHeaders<>(new DefaultHttp2Headers());
                    if (metadata.contentType() != null && metadata.contentType().startsWith("application/grpc")) {
                        headers.set(":status", "200")
                                .set("content-type", "application/grpc")
                                .set("grpc-status", "7")
                                .set("grpc-message", "xDS%20access%20denied");
                    } else {
                        headers.set(":status", "403").set("content-length", "0");
                    }
                    channel.writeHeader(new Http2MetadataFrame(headers, true)).whenComplete((ignored, failure) -> {
                        if (failure != null) {
                            channel.writeResetFrame(8);
                            channel.flush();
                        }
                    });
                    channel.flush();
                    return;
                }
            }
            if (!denied) {
                delegate.onMetadata(metadata);
            }
        }

        @Override
        public void onData(Http2InputMessage message) {
            if (!checked || denied) {
                try {
                    int bytes = message.getBody().available();
                    message.close();
                    channel.consumeBytes(bytes);
                } catch (Exception ignored) {
                    channel.writeResetFrame(8);
                    channel.flush();
                }
                return;
            }
            delegate.onData(message);
        }

        @Override
        public void cancelByRemote(long errorCode) {
            delegate.cancelByRemote(errorCode);
        }

        @Override
        public void close() {
            delegate.close();
        }

        @Override
        public void onWritabilityChanged() {
            if (!denied) {
                delegate.onWritabilityChanged();
            }
        }
    }
}
