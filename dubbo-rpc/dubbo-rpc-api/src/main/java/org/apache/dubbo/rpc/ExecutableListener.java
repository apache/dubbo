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

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.util.concurrent.Executor;


public class ExecutableListener implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ExecutableListener.class);

    private final Executor executor;
    private final CancellationListener listener;
    private final RpcServiceContext context;

    public ExecutableListener(Executor executor, CancellationListener listener, RpcServiceContext context) {
        this.executor = executor;
        this.listener = listener;
        this.context = context;
    }

    public ExecutableListener(Executor executor, CancellationListener listener) {
        this(executor, listener, null);
    }


    public void deliver() {
        try {
            executor.execute(this);
        } catch (Throwable t) {
            log.warn("Exception notifying context listener", t);
        }
    }

    public Executor getExecutor() {
        return executor;
    }

    public CancellationListener getListener() {
        return listener;
    }

    public RpcServiceContext getContext() {
        return context;
    }

    @Override
    public void run() {
        listener.cancelled(context);
    }
}
