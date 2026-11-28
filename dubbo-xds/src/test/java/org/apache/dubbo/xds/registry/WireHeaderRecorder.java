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
package org.apache.dubbo.xds.registry;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.remoting.http12.h2.Http2Header;
import org.apache.dubbo.remoting.http12.h2.Http2InputMessage;
import org.apache.dubbo.remoting.http12.h2.Http2MetadataFrame;
import org.apache.dubbo.remoting.http12.h2.Http2ServerTransportListenerDecorator;
import org.apache.dubbo.remoting.http12.h2.Http2TransportListener;
import org.apache.dubbo.remoting.http12.netty4.NettyHttpHeaders;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.netty.handler.codec.http2.DefaultHttp2Headers;

/** Test-only observation of headers received from the socket, before Triple method dispatch. */
@Activate
public final class WireHeaderRecorder implements Http2ServerTransportListenerDecorator {
    static final String HEADER = "x-xds-wire-test";
    private static final Map<String, Capture> CAPTURES = new ConcurrentHashMap<>();

    static Capture capture(String name) {
        Capture capture = new Capture(name);
        if (CAPTURES.putIfAbsent(name, capture) != null) {
            throw new IllegalStateException("Capture already registered");
        }
        return capture;
    }

    @Override
    public Http2TransportListener decorate(
            Http2TransportListener listener, H2StreamChannel channel, URL url, FrameworkModel framework) {
        return new Http2TransportListener() {
            private boolean rejected;

            public void onMetadata(Http2Header metadata) {
                String name = metadata.headers().getFirst(HEADER);
                Capture capture = name == null ? null : CAPTURES.get(name);
                if (capture != null) {
                    Map<String, String> received = new LinkedHashMap<>();
                    metadata.headers().forEach(received::put);
                    capture.headers.offer(received);
                    if (capture.reject) {
                        rejected = true;
                        channel.writeHeader(new Http2MetadataFrame(
                                new NettyHttpHeaders<>(new DefaultHttp2Headers()
                                        .status("200")
                                        .set("content-type", "application/grpc")
                                        .set("grpc-status", "7")),
                                true));
                        channel.flush();
                        return;
                    }
                }
                listener.onMetadata(metadata);
            }

            public void onData(Http2InputMessage message) {
                if (rejected) {
                    try {
                        int bytes = message.getBody().available();
                        message.close();
                        channel.consumeBytes(bytes);
                    } catch (Exception error) {
                        throw new IllegalStateException(error);
                    }
                } else listener.onData(message);
            }

            public void cancelByRemote(long errorCode) {
                listener.cancelByRemote(errorCode);
            }

            public void close() {
                listener.close();
            }

            public void onWritabilityChanged() {
                listener.onWritabilityChanged();
            }
        };
    }

    static final class Capture implements AutoCloseable {
        final String name;
        volatile boolean reject;
        final BlockingQueue<Map<String, String>> headers = new ArrayBlockingQueue<>(4);

        Capture(String name) {
            this.name = name;
        }

        Map<String, String> received() throws InterruptedException {
            Map<String, String> received = headers.poll(5, TimeUnit.SECONDS);
            if (received == null) {
                throw new AssertionError("No wire headers received for " + name);
            }
            return received;
        }

        public void close() {
            CAPTURES.remove(name, this);
        }
    }
}
