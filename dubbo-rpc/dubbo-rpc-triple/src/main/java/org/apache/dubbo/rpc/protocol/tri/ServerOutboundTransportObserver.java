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

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.protocol.tri.command.CancelQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.DataQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.HeaderQueueCommand;

import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;

public class ServerOutboundTransportObserver extends OutboundTransportObserver {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerOutboundTransportObserver.class);

    public ServerOutboundTransportObserver(WriteQueue queue) {
        super(queue);
    }

    public void onMetadata(Http2Headers headers, boolean endStream) {
        checkSendMeta(headers, endStream);
        writeQueue.enqueue(HeaderQueueCommand.createHeaders(headers, endStream), true)
            .addListener(future -> {
                if (!future.isSuccess()) {
                    LOGGER.warn("send header error endStream=" + endStream, future.cause());
                }
            });
    }

    @Override
    public void onMetadata(Metadata metadata, boolean endStream) {
        doOnMetadata(metadata, endStream);
    }

    @Override
    public void onData(byte[] data, boolean endStream) {
        doOnData(data, endStream);
    }

    @Override
    protected void doOnMetadata(Metadata metadata, boolean endStream) {
        final DefaultHttp2Headers headers = new DefaultHttp2Headers(true);
        metadata.forEach(e -> headers.set(e.getKey(), e.getValue()));
        onMetadata(headers, endStream);
    }

    @Override
    protected void doOnData(byte[] data, boolean endStream) {
        writeQueue.enqueue(DataQueueCommand.createGrpcCommand(data, endStream, false), true)
            .addListener(future -> {
                if (!future.isSuccess()) {
                    LOGGER.warn("send data error endStream=" + endStream, future.cause());
                }
            });
    }

    @Override
    protected void doOnError(GrpcStatus status) {
        writeQueue.enqueue(CancelQueueCommand.createCommand(status), true)
            .addListener(future -> {
                if (!future.isSuccess()) {
                    LOGGER.warn("write reset error", future.cause());
                }
            });
    }

    @Override
    protected void doOnComplete() {

    }
}
