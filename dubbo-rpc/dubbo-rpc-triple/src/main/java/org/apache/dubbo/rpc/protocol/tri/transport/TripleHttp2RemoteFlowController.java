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

import io.netty.handler.codec.http2.DefaultHttp2RemoteFlowController;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2RemoteFlowController;
import io.netty.handler.codec.http2.StreamByteDistributor;
import io.netty.handler.codec.http2.WeightedFairQueueByteDistributor;

/**
 * Triple-specific implementation of {@link Http2RemoteFlowController}.
 *
 * <p>This class extends the {@link DefaultHttp2RemoteFlowController} to provide
 * flow control management for Triple (Dubbo's gRPC-compatible protocol) connections.
 * It coordinates the distribution of outbound flow-controlled bytes across all
 * active streams within a connection.
 *
 * <p>The controller utilizes a {@link WeightedFairQueueByteDistributor} to ensure
 * that bandwidth is allocated based on stream weights and priorities while
 * maintaining fairness to prevent stream starvation.
 *
 * @see DefaultHttp2RemoteFlowController
 * @see WeightedFairQueueByteDistributor
 */
public class TripleHttp2RemoteFlowController extends DefaultHttp2RemoteFlowController {

    /**
     * Constructs a new TripleHttp2RemoteFlowController.
     *
     * @param connection the {@link Http2Connection} to be managed.
     * @param streamByteDistributor the distributor responsible for determining how
     * available bytes are allocated among streams.
     */
    public TripleHttp2RemoteFlowController(Http2Connection connection, StreamByteDistributor streamByteDistributor) {
        super(connection, streamByteDistributor);
    }

    /**
     * Factory method to create a pre-configured flow controller optimized for Triple performance.
     *
     * <p>Configuration details:
     * <ul>
     * <li>Uses {@link WeightedFairQueueByteDistributor} for weighted-fair resource allocation.</li>
     * <li>Sets {@code allocationQuantum} to 16KB. This setting reduces scheduling overhead
     * by ensuring each stream is allocated a meaningful chunk of data before switching
     * contexts, which significantly improves throughput in high-load scenarios.</li>
     * </ul>
     *
     * @param connection the {@link Http2Connection} for which the controller will be created.
     * @return a fully initialized {@link Http2RemoteFlowController} instance.
     */
    public static Http2RemoteFlowController newController(Http2Connection connection) {
        WeightedFairQueueByteDistributor dist = new WeightedFairQueueByteDistributor(connection);
        // Optimization: 16KB quantum size to balance fairness and high-throughput performance.
        dist.allocationQuantum(16 * 1024);
        return new TripleHttp2RemoteFlowController(connection, dist);
    }
}
