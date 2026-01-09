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
package org.apache.dubbo.rpc.protocol.tri.transport;

import io.netty.handler.codec.http2.DefaultHttp2LocalFlowController;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2LocalFlowController;

/**
 * Custom HTTP/2 local flow controller for Triple protocol with manual flow control.
 *
 * <p>This flow controller works together with {@code Http2StreamChannelOption.AUTO_STREAM_FLOW_CONTROL = false}
 * to enable manual flow control. The complete mechanism requires two parts:
 *
 * <h3>1. Disable Netty's automatic WINDOW_UPDATE (at Http2StreamChannel level)</h3>
 * <p>Set {@code Http2StreamChannelOption.AUTO_STREAM_FLOW_CONTROL = false} when creating Http2StreamChannel.
 * This prevents Netty's AbstractHttp2StreamChannel from automatically sending WINDOW_UPDATE frames.
 *
 * <h3>2. Manual flow control pattern (at Http2Connection level)</h3>
 * <ol>
 *   <li>Data is received and tracked via {@link #receiveFlowControlledFrame} - decreases window size</li>
 *   <li>Application processes the data (decoding, business logic)</li>
 *   <li>Application calls {@link #consumeBytes} to return processed bytes</li>
 *   <li>WINDOW_UPDATE frame is sent when consumed bytes reach threshold (default: 50% of initial window)</li>
 * </ol>
 *
 * <h3>Flow Control Call Chain</h3>
 * <pre>
 * Server: StreamingDecoder.bytesRead() → FragmentListener.bytesRead() → H2StreamChannel.consumeBytes()
 *         → Http2LocalFlowController.consumeBytes() → WINDOW_UPDATE
 * Client: TriDecoder.Listener.bytesRead() → AbstractTripleClientStream.consumeBytes()
 *         → Http2LocalFlowController.consumeBytes() → WINDOW_UPDATE
 * </pre>
 *
 * @see org.apache.dubbo.remoting.http12.h2.H2StreamChannel#consumeBytes(int)
 * @see io.netty.handler.codec.http2.Http2StreamChannelOption#AUTO_STREAM_FLOW_CONTROL
 */
public class TripleHttp2LocalFlowController extends DefaultHttp2LocalFlowController {

    /**
     * Creates a new flow controller with custom windowUpdateRatio.
     *
     * @param connection        the HTTP/2 connection
     * @param windowUpdateRatio the ratio of consumed bytes to initial window size at which
     *                          WINDOW_UPDATE frames are sent. Must be between 0 (exclusive) and 1 (inclusive).
     *                          For example, 0.5 means WINDOW_UPDATE is sent when 50% of the
     *                          initial window has been consumed.
     */
    public TripleHttp2LocalFlowController(Http2Connection connection, float windowUpdateRatio) {
        super(connection, windowUpdateRatio, true);
    }

    public static Http2LocalFlowController newController(Http2Connection connection, float windowUpdateRatio) {
        return new TripleHttp2LocalFlowController(connection, windowUpdateRatio);
    }
}
