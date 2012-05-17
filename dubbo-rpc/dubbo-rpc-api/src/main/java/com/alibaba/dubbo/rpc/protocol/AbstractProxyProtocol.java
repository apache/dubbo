/*
 * Copyright 1999-2012 Alibaba Group.
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
package com.alibaba.dubbo.rpc.protocol;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.RpcException;

/**
 * AbstractProxyProtocol
 * 
 * @author william.liangf
 */
public abstract class AbstractProxyProtocol extends AbstractProtocol {

    private ProxyFactory proxyFactory;

    public void setProxyFactory(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    public <T> Exporter<T> export(final Invoker<T> invoker) throws RpcException {
        final Runnable runnable = doExport(proxyFactory.getProxy(invoker), invoker.getInterface(), invoker.getUrl());
        return new Exporter<T>() {
            public Invoker<T> getInvoker() {
                return invoker;
            }
            public void unexport() {
                runnable.run();
            }
        };
    }

    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
        return proxyFactory.getInvoker(doRefer(type, url), type, url);
    }

    public abstract <T> Runnable doExport(T impl, Class<T> type, URL url) throws RpcException;

    public abstract <T> T doRefer(Class<T> type, URL url) throws RpcException;

}
