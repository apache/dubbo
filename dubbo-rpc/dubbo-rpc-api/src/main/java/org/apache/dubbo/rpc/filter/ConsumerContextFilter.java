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
package org.apache.dubbo.rpc.filter;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.AbstractPostProcessFilter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;

/**
 * ConsumerContextInvokerFilter
 */
@Activate(group = Constants.CONSUMER, order = -10000)
public class ConsumerContextFilter extends AbstractPostProcessFilter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        RpcContext.getContext()
                .setInvoker(invoker)
                .setInvocation(invocation)
                .setLocalAddress(NetUtils.getLocalHost(), 0)
                .setRemoteAddress(invoker.getUrl().getHost(),
                        invoker.getUrl().getPort());
        if (invocation instanceof RpcInvocation) {
            ((RpcInvocation) invocation).setInvoker(invoker);
        }
        try {
            // TODO should we clear server context?
            RpcContext.removeServerContext();
            return postProcessResult(invoker.invoke(invocation), invoker, invocation);
        } finally {
            // TODO removeContext? but we need to save future for RpcContext.getFuture() API. If clear attachments here, attachments will not available when postProcessResult is invoked.
            RpcContext.getContext().clearAttachments();
        }
    }

    @Override
    protected Result doPostProcess(Result result, Invoker<?> invoker, Invocation invocation) {
        RpcContext.getServerContext().setAttachments(result.getAttachments());
        return result;
    }
}
