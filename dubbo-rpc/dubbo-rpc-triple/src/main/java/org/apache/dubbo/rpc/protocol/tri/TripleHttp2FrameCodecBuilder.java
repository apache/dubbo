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

package org.apache.dubbo.rpc.protocol.tri;

import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2LocalFlowController;
import io.netty.handler.codec.http2.Http2RemoteFlowController;
import org.apache.dubbo.common.utils.Assert;

import java.util.function.Consumer;

public class TripleHttp2FrameCodecBuilder extends Http2FrameCodecBuilder {

    TripleHttp2FrameCodecBuilder(Http2Connection connection) {
        connection(connection);
    }

    public static TripleHttp2FrameCodecBuilder fromConnection(Http2Connection connection) {
        return new TripleHttp2FrameCodecBuilder(connection);
    }

    public static TripleHttp2FrameCodecBuilder forClient() {
        return forClient(Http2CodecUtil.SMALLEST_MAX_CONCURRENT_STREAMS);
    }

    public static TripleHttp2FrameCodecBuilder forClient(int maxReservedStreams) {
        return fromConnection(new DefaultHttp2Connection(false, maxReservedStreams));
    }

    public static TripleHttp2FrameCodecBuilder forServer() {
        return forServer(Http2CodecUtil.SMALLEST_MAX_CONCURRENT_STREAMS);
    }

    public static TripleHttp2FrameCodecBuilder forServer(int maxReservedStreams) {
        return fromConnection(new DefaultHttp2Connection(true, maxReservedStreams));
    }

    public TripleHttp2FrameCodecBuilder customizeConnection(Consumer<Http2Connection> connectionCustomizer) {
        Http2Connection connection = this.connection();
        Assert.notNull(connection, "connection cannot be null.");
        connectionCustomizer.accept(connection);
        return this;
    }

    public TripleHttp2FrameCodecBuilder remoteFlowController(Http2RemoteFlowController remoteFlowController) {
        return this.customizeConnection((connection) -> connection.remote().flowController(remoteFlowController));
    }

    public TripleHttp2FrameCodecBuilder localFlowController(Http2LocalFlowController localFlowController) {
        return this.customizeConnection((connection) -> connection.local().flowController(localFlowController));
    }
}
