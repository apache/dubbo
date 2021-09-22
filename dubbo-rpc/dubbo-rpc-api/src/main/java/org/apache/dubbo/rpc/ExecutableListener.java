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

import java.util.concurrent.Executor;

/**
 * @author earthchen
 * @date 2021/9/20
 **/
public class ExecutableListener implements Runnable {

    private final Executor executor;
    private final CancellationListener listener;
    private final CancellableContext context;

    public ExecutableListener(Executor executor, CancellationListener listener, CancellableContext context) {
        this.executor = executor;
        this.listener = listener;
        this.context = context;
    }

    public void deliver() {
        try {
            executor.execute(this);
        } catch (Throwable t) {
//            log.log(Level.INFO, "Exception notifying context listener", t);
        }
    }

    public Executor getExecutor() {
        return executor;
    }

    public CancellationListener getListener() {
        return listener;
    }

    public RpcContext getContext() {
        return context;
    }

    @Override
    public void run() {
        listener.cancelled(context);
    }
}
