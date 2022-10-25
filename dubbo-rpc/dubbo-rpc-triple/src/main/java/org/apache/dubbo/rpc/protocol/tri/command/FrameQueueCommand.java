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

package org.apache.dubbo.rpc.protocol.tri.command;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.Http2StreamFrame;

public class FrameQueueCommand {

    private ChannelPromise promise;

    private final Http2StreamFrame frame;

    private FrameQueueCommand(Http2StreamFrame http2StreamFrame) {
        this.frame = http2StreamFrame;
    }

    public static FrameQueueCommand createGrpcCommand(Http2StreamFrame http2StreamFrame) {
        return new FrameQueueCommand(http2StreamFrame);
    }

    public ChannelPromise promise() {
        return promise;
    }

    public void promise(ChannelPromise promise) {
        this.promise = promise;
    }

    public void cancel() {
        promise.tryFailure(new IllegalStateException("Canceled"));
    }

    public final Http2StreamFrame getFrame() {
        return frame;
    }

    public void run(Channel channel) {
        channel.write(frame, promise);
    }
}
