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

package org.apache.dubbo.rpc.stub;

import java.util.concurrent.CompletableFuture;

/**
 * A generic methodHandler for stub invocation
 *
 * @param <T> Request Type
 * @param <R> Response Type
 */
public interface StubMethodHandler<T, R> {

    /**
     * Invoke method
     *
     * @param arguments may contain {@link org.apache.dubbo.common.stream.StreamObserver} or just
     *                  single request instance.
     * @return an Async or Sync future
     */
    CompletableFuture<?> invoke(Object[] arguments);
}

