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
package org.apache.dubbo.remoting.http12.h2.command;

import org.apache.dubbo.remoting.http12.HttpMetadata;
import org.apache.dubbo.remoting.http12.HttpOutputMessage;
import org.apache.dubbo.remoting.http12.command.DataQueueCommand;
import org.apache.dubbo.remoting.http12.command.HeaderQueueCommand;
import org.apache.dubbo.remoting.http12.command.HttpWriteQueue;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.remoting.http12.h2.Http2ChannelDelegate;

import java.util.concurrent.CompletableFuture;

public class Http2WriteQueueChannel extends Http2ChannelDelegate {

    private final HttpWriteQueue httpWriteQueue;

    public Http2WriteQueueChannel(H2StreamChannel h2StreamChannel, HttpWriteQueue httpWriteQueue) {
        super(h2StreamChannel);
        this.httpWriteQueue = httpWriteQueue;
    }

    @Override
    public CompletableFuture<Void> writeHeader(HttpMetadata httpMetadata) {
        HeaderQueueCommand cmd = new HeaderQueueCommand(httpMetadata);
        cmd.setHttpChannel(this::getH2StreamChannel);
        return httpWriteQueue.enqueue(cmd);
    }

    @Override
    public CompletableFuture<Void> writeMessage(HttpOutputMessage httpOutputMessage) {
        DataQueueCommand cmd = new DataQueueCommand(httpOutputMessage);
        cmd.setHttpChannel(this::getH2StreamChannel);
        return httpWriteQueue.enqueue(cmd);
    }

    @Override
    public CompletableFuture<Void> writeResetFrame(long errorCode) {
        ResetQueueCommand cmd = new ResetQueueCommand(errorCode);
        cmd.setHttpChannel(this::getH2StreamChannel);
        return this.httpWriteQueue.enqueue(cmd);
    }
}
