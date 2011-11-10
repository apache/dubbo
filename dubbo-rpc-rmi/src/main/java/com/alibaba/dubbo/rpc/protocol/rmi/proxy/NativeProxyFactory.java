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
package com.alibaba.dubbo.rpc.protocol.rmi.proxy;

import java.rmi.Remote;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;

import com.alibaba.dubbo.common.Extension;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.rmi.RmiProxyFactory;

/**
 * DefaultProxyFactoryAdaptive
 * 
 * @author william.liangf
 */
@Extension("native")
public class NativeProxyFactory implements RmiProxyFactory {

    private ProxyFactory                      proxyFactory;
    
    public void setProxyFactory(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    public <T> Remote getProxy(final Invoker<T> invoker) {
        return (Remote) proxyFactory.getProxy(new Invoker<T>() {
            public Class<T> getInterface() {
                return invoker.getInterface();
            }

            public URL getUrl() {
                return invoker.getUrl();
            }

            public boolean isAvailable() {
                return true;
            }

            public Result invoke(Invocation invocation) throws RpcException {
                String client = null;
                try {
                    client = RemoteServer.getClientHost();
                } catch (ServerNotActiveException e) {
                    // Ignore it.
                }
                RpcContext.getContext().setRemoteAddress(client, 0);
                return invoker.invoke(invocation);
            }

            public void destroy() {
            }
        });
    }

    public boolean isSupported(Remote remote, Class<?> serviceType, URL url) {
        return Remote.class.isAssignableFrom(serviceType) && serviceType.isInstance(remote);
    }

    @SuppressWarnings("unchecked")
    public <T> Invoker<T> getInvoker(Remote remote, Class<T> serviceType, URL url) {
        return proxyFactory.getInvoker((T) remote, serviceType, url);
    }

}
