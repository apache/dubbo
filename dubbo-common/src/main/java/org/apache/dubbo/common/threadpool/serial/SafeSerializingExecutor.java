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

package org.apache.dubbo.common.threadpool.serial;

import java.util.concurrent.Executor;

import static org.apache.dubbo.common.utils.ExecutorUtil.isShutdown;

/**
 * If the thread pool is in a shutdown state, won't submit task, so it doesn't throw reject exception
 */
public class SafeSerializingExecutor extends SerializingExecutor {

    /**
     * Creates a SerializingExecutor, running tasks using {@code executor}.
     *
     * @param executor Executor in which tasks should be run. Must not be null.
     */
    public SafeSerializingExecutor(Executor executor) {
        super(executor);
    }

    @Override
    protected boolean submitTask() {
        if (isShutdown(executor)) {
            return false;
        }
        return super.submitTask();
    }
}
