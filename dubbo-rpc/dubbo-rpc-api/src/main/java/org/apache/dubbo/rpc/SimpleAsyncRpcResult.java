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
 * A sub class used for normal async invoke.
 *
 * <b>NOTICE!!</b>
 *
 * <p>
 * You should never rely on this class directly when using or extending Dubbo, the implementation of {@link SimpleAsyncRpcResult}
 * is only a workaround for compatibility purpose. It may be changed or even get removed from the next major version.
 * Please only use {@link Result} or {@link RpcResult}.
 * </p>
 *
 * Check {@link AsyncRpcResult} for more details.
 *
 * TODO AsyncRpcResult, AsyncNormalRpcResult should not be a parent-child hierarchy.
 */
public class SimpleAsyncRpcResult extends AsyncRpcResult {
    public SimpleAsyncRpcResult(CompletableFuture<Object> future, boolean registerCallback) {
        super(future, registerCallback);
    }

    public SimpleAsyncRpcResult(CompletableFuture<Object> future, CompletableFuture<Result> rFuture, boolean registerCallback) {
        super(future, rFuture, registerCallback);
    }

    @Override
    public Object recreate() throws Throwable {
        // TODO should we check the status of valueFuture here?
        return null;
    }
}
