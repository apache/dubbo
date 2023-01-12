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
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.InvokeMode;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.PenetrateAttachmentSelector;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.TimeoutCountDown;
import org.apache.dubbo.rpc.cluster.filter.ClusterFilter;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.support.RpcUtils;

import java.util.Map;
import java.util.Set;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER;
import static org.apache.dubbo.common.constants.CommonConstants.ENABLE_TIMEOUT_COUNTDOWN_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIME_COUNTDOWN_KEY;

/**
 * ConsumerContextFilter set current RpcContext with invoker,invocation, local host, remote host and port
 * for consumer invoker.It does it to make the requires info available to execution thread's RpcContext.
 *
 * @see Filter
 * @see RpcContext
 */
@Activate(group = CONSUMER, order = Integer.MIN_VALUE)
public class ConsumerContextFilter implements ClusterFilter, ClusterFilter.Listener {

    private Set<PenetrateAttachmentSelector> supportedSelectors;

    public ConsumerContextFilter(ApplicationModel applicationModel) {
        ExtensionLoader<PenetrateAttachmentSelector> selectorExtensionLoader = applicationModel.getExtensionLoader(PenetrateAttachmentSelector.class);
        supportedSelectors = selectorExtensionLoader.getSupportedExtensionInstances();
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        RpcContext.RestoreServiceContext originServiceContext = RpcContext.storeServiceContext();
        try {
            RpcContext.getServiceContext()
                .setInvoker(invoker)
                .setInvocation(invocation)
                .setLocalAddress(NetUtils.getLocalHost(), 0);

            RpcContext context = RpcContext.getClientAttachment();
            context.setAttachment(REMOTE_APPLICATION_KEY, invoker.getUrl().getApplication());
            if (invocation instanceof RpcInvocation) {
                ((RpcInvocation) invocation).setInvoker(invoker);
            }

            if (CollectionUtils.isNotEmpty(supportedSelectors)) {
                for (PenetrateAttachmentSelector supportedSelector : supportedSelectors) {
                    Map<String, Object> selected = supportedSelector.select(invocation, RpcContext.getClientAttachment(), RpcContext.getServerAttachment());
                    if (CollectionUtils.isNotEmptyMap(selected)) {
                        ((RpcInvocation) invocation).addObjectAttachments(selected);
                    }
                }
            } else {
                ((RpcInvocation) invocation).addObjectAttachments(RpcContext.getServerAttachment().getObjectAttachments());
            }
            Map<String, Object> contextAttachments = RpcContext.getClientAttachment().getObjectAttachments();
            if (CollectionUtils.isNotEmptyMap(contextAttachments)) {
                /**
                 * invocation.addAttachmentsIfAbsent(context){@link RpcInvocation#addAttachmentsIfAbsent(Map)}should not be used here,
                 * because the {@link RpcContext#setAttachment(String, String)} is passed in the Filter when the call is triggered
                 * by the built-in retry mechanism of the Dubbo. The attachment to update RpcContext will no longer work, which is
                 * a mistake in most cases (for example, through Filter to RpcContext output traceId and spanId and other information).
                 */
                ((RpcInvocation) invocation).addObjectAttachments(contextAttachments);
            }

            // pass default timeout set by end user (ReferenceConfig)
            Object countDown = RpcContext.getServerAttachment().getObjectAttachment(TIME_COUNTDOWN_KEY);
            if (countDown != null) {
                String methodName = RpcUtils.getMethodName(invocation);
                // When the client has enabled the timeout-countdown function,
                // the subsequent calls launched by the Server side will be enabled by default,
                // and support to turn off the function on a node to get rid of the timeout control.
                if (invoker.getUrl().getMethodParameter(methodName, ENABLE_TIMEOUT_COUNTDOWN_KEY, true)) {
                    context.setObjectAttachment(TIME_COUNTDOWN_KEY, countDown);

                    TimeoutCountDown timeoutCountDown = (TimeoutCountDown) countDown;
                    if (timeoutCountDown.isExpired()) {
                        return AsyncRpcResult.newDefaultAsyncResult(new RpcException(RpcException.TIMEOUT_TERMINATE,
                            "No time left for making the following call: " + invocation.getServiceName() + "."
                                + invocation.getMethodName() + ", terminate directly."), invocation);
                    }
                }
            }
            RpcContext.removeClientResponseContext();
            return invoker.invoke(invocation);
        } finally {
            RpcContext.restoreServiceContext(originServiceContext);
        }
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
        removeContext(invocation);
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
