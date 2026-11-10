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
package org.apache.dubbo.remoting.http12.command;

import org.apache.dubbo.common.BatchExecutorQueue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class HttpWriteQueue extends BatchExecutorQueue<HttpChannelQueueCommand> {

    private final Executor executor;

    private final Runnable batchFlush;

    public HttpWriteQueue(Executor executor) {
        this(executor, null);
    }

    public HttpWriteQueue(Executor executor, Runnable batchFlush) {
        this.executor = executor;
        this.batchFlush = batchFlush;
    }

    public CompletableFuture<Void> enqueue(HttpChannelQueueCommand cmd) {
        this.enqueue(cmd, this.executor);
        return cmd;
    }

    @Override
    protected void prepare(HttpChannelQueueCommand item) {
        item.run();
    }

    @Override
    protected void flush(HttpChannelQueueCommand item) {
        item.run();
        if (batchFlush != null) {
            batchFlush.run();
        } else {
            item.getHttpChannel().flush();
        }
    }
}
