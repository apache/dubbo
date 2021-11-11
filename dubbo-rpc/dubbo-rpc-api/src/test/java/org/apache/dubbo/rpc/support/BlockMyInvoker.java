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
package org.apache.dubbo.rpc.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

public class BlockMyInvoker<T> extends MyInvoker<T> {

    private long blockTime = 100;

    public BlockMyInvoker(URL url, long blockTime) {
        super(url);
        this.blockTime = blockTime;
    }

    public BlockMyInvoker(URL url, boolean hasException, long blockTime) {
        super(url, hasException);
        this.blockTime = blockTime;
    }

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        AppResponse result = new AppResponse();
        if (!hasException) {
            try {
                Thread.sleep(blockTime);
            } catch (InterruptedException e) {
            }
            result.setValue("Dubbo");
        } else {
            result.setException(new RuntimeException("mocked exception"));
        }

        return AsyncRpcResult.newDefaultAsyncResult(result, invocation);
    }

    public long getBlockTime() {
        return blockTime;
    }

    public void setBlockTime(long blockTime) {
        this.blockTime = blockTime;
    }
}
