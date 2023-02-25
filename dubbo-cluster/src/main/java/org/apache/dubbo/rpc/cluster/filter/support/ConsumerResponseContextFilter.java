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
package org.apache.dubbo.rpc.cluster.filter.support;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.BaseFilter;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.InvokeMode;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;

import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER;

/**
 * Pass the context returned by the provider, the largest order value makes the first call when returning,
 * otherwise the subsequent filter will not get the context
 *
 * @see Filter
 * @see RpcContext
 */
@Activate(group = CONSUMER, order = Integer.MAX_VALUE)
public class ConsumerResponseContextFilter implements Filter, BaseFilter.Listener {


    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        return invoker.invoke(invocation);
    }

    @Override
    public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {
        // pass attachments to result
        Map<String, Object> map = appResponse.getObjectAttachments();
        RpcContext.getClientResponseContext().setObjectAttachments(map);
        removeContext(invocation);
    }

    @Override
    public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {
    }

    private void removeContext(Invocation invocation) {
        RpcContext.removeClientAttachment();
        if (invocation instanceof RpcInvocation) {
            RpcInvocation rpcInvocation = (RpcInvocation) invocation;
            if (rpcInvocation.getInvokeMode() != null) {
                // clear service context if not in sync mode
                if (rpcInvocation.getInvokeMode() == InvokeMode.ASYNC || rpcInvocation.getInvokeMode() == InvokeMode.FUTURE) {
                    RpcContext.removeServiceContext();
                }
            }
        }
        // server context must not be removed because user might use it on callback.
        // So the clear of is delayed til the start of the next rpc call, see RpcContext.removeServerContext(); in invoke() above
        // RpcContext.removeServerContext();
    }

}
