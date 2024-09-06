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
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.PenetrateAttachmentSelector;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.TimeoutCountDown;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.support.RpcUtils;
import org.apache.dubbo.rpc.support.TrieTree;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_VERSION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER;
import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TAG_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_ATTACHMENT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIME_COUNTDOWN_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.rpc.Constants.ASYNC_KEY;
import static org.apache.dubbo.rpc.Constants.FORCE_USE_TAG;
import static org.apache.dubbo.rpc.Constants.TOKEN_KEY;

/**
 * ContextFilter set the provider RpcContext with invoker, invocation, local port it is using and host for
 * current execution thread.
 *
 * @see RpcContext
 */
@Activate(group = PROVIDER, order = Integer.MIN_VALUE)
public class ContextFilter implements Filter, Filter.Listener {
    private final Set<PenetrateAttachmentSelector> supportedSelectors;

    public ContextFilter(ApplicationModel applicationModel) {
        ExtensionLoader<PenetrateAttachmentSelector> selectorExtensionLoader =
                applicationModel.getExtensionLoader(PenetrateAttachmentSelector.class);
        supportedSelectors = selectorExtensionLoader.getSupportedExtensionInstances();
    }

    private static final TrieTree UNLOADING_KEYS;

    static {
        Set<String> keySet = new HashSet<>();
        keySet.add(PATH_KEY);
        keySet.add(INTERFACE_KEY);
        keySet.add(GROUP_KEY);
        keySet.add(VERSION_KEY);
        keySet.add(DUBBO_VERSION_KEY);
        keySet.add(TOKEN_KEY);
        keySet.add(TIMEOUT_KEY);
        keySet.add(TIMEOUT_ATTACHMENT_KEY);

        // Remove async property to avoid being passed to the following invoke chain.
        keySet.add(ASYNC_KEY);
        keySet.add(TAG_KEY);
        keySet.add(FORCE_USE_TAG);

        // Remove HTTP headers to avoid being passed to the following invoke chain.
        keySet.add("accept");
        keySet.add("accept-charset");
        keySet.add("accept-datetime");
        keySet.add("accept-encoding");
        keySet.add("accept-language");
        keySet.add("access-control-request-headers");
        keySet.add("access-control-request-method");
        keySet.add("authorization");
        keySet.add("cache-control");
        keySet.add("connection");
        keySet.add("content-length");
        keySet.add("content-md5");
        keySet.add("content-type");
        keySet.add("cookie");
        keySet.add("date");
        keySet.add("dnt");
        keySet.add("expect");
        keySet.add("forwarded");
        keySet.add("from");
        keySet.add("host");
        keySet.add("http2-settings");
        keySet.add("if-match");
        keySet.add("if-modified-since");
        keySet.add("if-none-match");
        keySet.add("if-range");
        keySet.add("if-unmodified-since");
        keySet.add("max-forwards");
        keySet.add("origin");
        keySet.add("pragma");
        keySet.add("proxy-authorization");
        keySet.add("range");
        keySet.add("referer");
        keySet.add("sec-fetch-dest");
        keySet.add("sec-fetch-mode");
        keySet.add("sec-fetch-site");
        keySet.add("sec-fetch-user");
        keySet.add("te");
        keySet.add("trailer");
        keySet.add("upgrade");
        keySet.add("upgrade-insecure-requests");
        keySet.add("user-agent");

        UNLOADING_KEYS = new TrieTree(keySet);
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        Map<String, Object> attachments = invocation.getObjectAttachments();
        if (attachments != null) {
            Map<String, Object> newAttach = new HashMap<>(attachments.size());
            for (Map.Entry<String, Object> entry : attachments.entrySet()) {
                String key = entry.getKey();
                if (!UNLOADING_KEYS.search(key)) {
                    newAttach.put(key, entry.getValue());
                }
            }
            attachments = newAttach;
        }

        RpcContext.getServiceContext().setInvoker(invoker).setInvocation(invocation);

        RpcContext context = RpcContext.getServerAttachment();
        //                .setAttachments(attachments)  // merged from dubbox
        if (context.getLocalAddress() == null) {
            context.setLocalAddress(invoker.getUrl().getHost(), invoker.getUrl().getPort());
        }

        String remoteApplication = invocation.getAttachment(REMOTE_APPLICATION_KEY);
        if (StringUtils.isNotEmpty(remoteApplication)) {
            RpcContext.getServiceContext().setRemoteApplicationName(remoteApplication);
        } else {
            RpcContext.getServiceContext().setRemoteApplicationName(context.getAttachment(REMOTE_APPLICATION_KEY));
        }

        long timeout = RpcUtils.getTimeout(invocation, -1);
        if (timeout != -1) {
            // pass to next hop
            RpcContext.getServerAttachment()
                    .setObjectAttachment(
                            TIME_COUNTDOWN_KEY, TimeoutCountDown.newCountDown(timeout, TimeUnit.MILLISECONDS));
        }

        // merged from dubbox
        // we may already add some attachments into RpcContext before this filter (e.g. in rest protocol)
        if (CollectionUtils.isNotEmptyMap(attachments)) {
            if (context.getObjectAttachments().size() > 0) {
                context.getObjectAttachments().putAll(attachments);
            } else {
                context.setObjectAttachments(attachments);
            }
        }

        if (invocation instanceof RpcInvocation) {
            RpcInvocation rpcInvocation = (RpcInvocation) invocation;
            rpcInvocation.setInvoker(invoker);
        }

        try {
            context.clearAfterEachInvoke(false);
            return invoker.invoke(invocation);
        } finally {
            context.clearAfterEachInvoke(true);
            if (context.isAsyncStarted()) {
                removeContext();
            }
        }
    }

    @Override
    public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {
        // pass attachments to result
        if (CollectionUtils.isNotEmpty(supportedSelectors)) {
            for (PenetrateAttachmentSelector supportedSelector : supportedSelectors) {
                Map<String, Object> selected = supportedSelector.selectReverse(
                        invocation, RpcContext.getClientResponseContext(), RpcContext.getServerResponseContext());
                if (CollectionUtils.isNotEmptyMap(selected)) {
                    appResponse.addObjectAttachments(selected);
                }
            }
        } else {
            appResponse.addObjectAttachments(
                    RpcContext.getClientResponseContext().getObjectAttachments());
        }
        appResponse.addObjectAttachments(RpcContext.getServerResponseContext().getObjectAttachments());
        removeContext();
    }

    @Override
    public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {
        removeContext();
    }

    private void removeContext() {
        RpcContext.removeServerAttachment();
        RpcContext.removeClientAttachment();
        RpcContext.removeServiceContext();
        RpcContext.removeClientResponseContext();
        RpcContext.removeServerResponseContext();
    }
}
