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

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.ListenableFilter;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;

import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER;
import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.rpc.Constants.ASYNC_KEY;
import static org.apache.dubbo.remoting.Constants.DUBBO_VERSION_KEY;
import static org.apache.dubbo.rpc.Constants.FORCE_USE_TAG;
import static org.apache.dubbo.rpc.Constants.TOKEN_KEY;


/**
 * ContextFilter set the provider RpcContext with invoker, invocation, local port it is using and host for
 * current execution thread.
 *
 * @see RpcContext
 */
@Activate(group = PROVIDER, order = -10000)
public class ContextFilter extends ListenableFilter {
    private static final String TAG_KEY = "dubbo.tag";

    public ContextFilter() {
        super.listener = new ContextListener();
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        Map<String, String> attachments = invocation.getAttachments();
        if (attachments != null) {
            attachments = new HashMap<>(attachments);
            attachments.remove(PATH_KEY);
            attachments.remove(INTERFACE_KEY);
            attachments.remove(GROUP_KEY);
            attachments.remove(VERSION_KEY);
            attachments.remove(DUBBO_VERSION_KEY);
            attachments.remove(TOKEN_KEY);
            attachments.remove(TIMEOUT_KEY);
            // Remove async property to avoid being passed to the following invoke chain.
            attachments.remove(ASYNC_KEY);
            attachments.remove(TAG_KEY);
            attachments.remove(FORCE_USE_TAG);
        }
        RpcContext.getContext()
                .setInvoker(invoker)
                .setInvocation(invocation)
//                .setAttachments(attachments)  // merged from dubbox
                .setLocalAddress(invoker.getUrl().getHost(), invoker.getUrl().getPort())
                .setRemoteApplicationName(invoker.getUrl().getParameter(REMOTE_APPLICATION_KEY));

        // merged from dubbox
        // we may already added some attachments into RpcContext before this filter (e.g. in rest protocol)
        if (attachments != null) {
            if (RpcContext.getContext().getAttachments() != null) {
                RpcContext.getContext().getAttachments().putAll(attachments);
            } else {
                RpcContext.getContext().setAttachments(attachments);
            }
        }

        if (invocation instanceof RpcInvocation) {
            ((RpcInvocation) invocation).setInvoker(invoker);
        }
        try {
            return invoker.invoke(invocation);
        } finally {
            // IMPORTANT! For async scenario, we must remove context from current thread, so we always create a new RpcContext for the next invoke for the same thread.
            RpcContext.removeContext();
            RpcContext.removeServerContext();
        }
    }

    static class ContextListener implements Listener {
        @Override
        public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {
            // pass attachments to result
            appResponse.addAttachments(RpcContext.getServerContext().getAttachments());
        }

        @Override
        public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {

        }
    }
}
