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

import org.apache.dubbo.common.threadlocal.InternalThreadLocal;

import java.util.concurrent.CompletableFuture;

/**
 * Used for async call scenario. But if the method you are calling has a {@link CompletableFuture<?>} signature
 * you do not need to use this class since you will get a Future response directly.
 * <p>
 * Remember to save the Future reference before making another call using the same thread, otherwise,
 * the current Future will be override by the new one, which means you will lose the chance get the return value.
 */
public class FutureContext {

    public static InternalThreadLocal<CompletableFuture<?>> futureTL = new InternalThreadLocal<>();

    /**
     * get future.
     *
     * @param <T>
     * @return future
     */
    @SuppressWarnings("unchecked")
    public static <T> CompletableFuture<T> getCompletableFuture() {
        return (CompletableFuture<T>) futureTL.get();
    }

    /**
     * set future.
     *
     * @param future
     */
    public static void setFuture(CompletableFuture<?> future) {
        futureTL.set(future);
    }

}
