/*
 * Copyright 1999-2011 Alibaba Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.config.local;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class AdaptiveLocalInvoker<T> implements Invoker<T> {

    private static final Protocol PROTOCOL = ExtensionLoader.getExtensionLoader(
            Protocol.class).getAdaptiveExtension();

    private Invoker<T> invoker;

    private Invoker<T> localInvoker;

    public AdaptiveLocalInvoker(Invoker<T> invoker) {
        if (invoker == null) {
            throw new NullPointerException("invoker = null");
        }
        this.invoker = invoker;
    }

    public Class<T> getInterface() {
        return invoker.getInterface();
    }

    public Result invoke(Invocation invocation) throws RpcException {
        if (isLocal() && getLocalInvoker() != null) {
            return getLocalInvoker().invoke(invocation);
        }
        return invoker.invoke(invocation);
    }

    public URL getUrl() {
        if (isLocal() && getLocalInvoker() != null) {
            return getLocalInvoker().getUrl();
        }
        return invoker.getUrl();
    }

    public boolean isAvailable() {
        if (isLocal() && getLocalInvoker() != null) {
            return getLocalInvoker().isAvailable();
        }
        return invoker.isAvailable();
    }

    public void destroy() {
        if (isLocal() && getLocalInvoker() != null) {
            getLocalInvoker().destroy();
        }
        invoker.destroy();
    }

    private boolean isLocal() {
        return !invoker.getUrl().getParameter(Constants.REMOTE_KEY, false)
                && LocalServiceStore.getInstance().isRegistered(
                invoker.getUrl().getServiceKey());
    }

    private Invoker<T> getLocalInvoker() {
        if (localInvoker == null) {
            try {
                localInvoker = PROTOCOL.refer(
                        invoker.getInterface(),
                        new URL(Constants.LOCAL_PROTOCOL,
                                NetUtils.LOCALHOST, 0,
                                invoker.getUrl().getParameter(
                                        Constants.INTERFACE_KEY),
                                invoker.getUrl().getParameters()));
            } catch (Throwable e) { /* ignore */ }
        }
        return localInvoker;
    }

}
