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

import org.apache.dubbo.rpc.protocol.tri.command.CancelQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.DataQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.HeaderQueueCommand;

import io.netty.channel.ChannelPromise;

/**
 * Send stream data to remote
 * {@link ClientOutboundTransportObserver#promise} will be set success after rst or complete sent,
 */
public class ClientOutboundTransportObserver extends OutboundTransportObserver {

    private final ChannelPromise promise;

    public ClientOutboundTransportObserver(WriteQueue writeQueue, ChannelPromise promise) {
        super(writeQueue);
        this.promise = promise;
    }

    @Override
    protected void doOnMetadata(Metadata metadata, boolean endStream) {
        writeQueue.enqueue(HeaderQueueCommand.createHeaders(metadata, endStream), true)
            .addListener(future -> {
                if (!future.isSuccess()) {
                    promise.tryFailure(future.cause());
                }
            });
    }

    @Override
    protected void doOnData(byte[] data, boolean endStream) {
        writeQueue.enqueue(DataQueueCommand.createGrpcCommand(data, endStream, true), true)
            .addListener(future -> {
                if (!future.isSuccess()) {
                    promise.tryFailure(future.cause());
                }
            });
    }

    @Override
    protected void doOnError(GrpcStatus status) {
        writeQueue.enqueue(CancelQueueCommand.createCommand(status), true)
            .addListener(future -> {
                if (future.isSuccess()) {
                    promise.trySuccess();
                } else {
                    promise.tryFailure(future.cause());
                }
            });
    }

    @Override
    protected void doOnComplete() {
        writeQueue.enqueue(DataQueueCommand.createGrpcCommand(true), true)
            .addListener(future -> {
                if (future.isSuccess()) {
                    promise.trySuccess();
                } else {
                    promise.tryFailure(future.cause());
                }
            });
    }
}
