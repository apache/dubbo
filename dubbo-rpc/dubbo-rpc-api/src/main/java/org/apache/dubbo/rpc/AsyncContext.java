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
package org.apache.dubbo.rpc;

import java.util.concurrent.CompletableFuture;

/**
 * AsyncContext works like {@see javax.servlet.AsyncContext} in the Servlet 3.0.
 * An AsyncContext is stated by a call to {@link RpcContext#startAsync()}.
 * <p>
 * The demo is {@see com.alibaba.dubbo.examples.async.AsyncConsumer}
 * and {@see com.alibaba.dubbo.examples.async.AsyncProvider}
 */
public interface AsyncContext {

    CompletableFuture getInternalFuture();

    /**
     * write value and complete the async context.
     *
     * @param value invoke result
     */
    void write(Object value);

    /**
     * @return true if the aysnc context is started
     */
    boolean isAsyncStarted();

    /**
     * change the context state to stop
     */
    boolean stop();

    /**
     * change the context state to start
     */
    void start();

    void signalContextSwitch();
}
