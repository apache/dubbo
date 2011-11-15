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

import java.lang.reflect.InvocationTargetException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;

import org.springframework.remoting.rmi.RmiInvocationHandler;
import org.springframework.remoting.support.RemoteInvocation;

import com.alibaba.dubbo.common.Extension;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.RpcResult;
import com.alibaba.dubbo.rpc.protocol.rmi.RmiProxyFactory;

/**
 * SpringRmiProxyFactory
 * 
 * @author william.liangf
 */
@Extension("spring")
public class SpringRmiProxyFactory implements RmiProxyFactory {

    public boolean isSupported(Remote remote, Class<?> serviceType, URL url) {
        return ReflectUtils.isInstance(remote, "org.springframework.remoting.rmi.RmiInvocationHandler");
    }
    
    private static void assertRmiInvocationHandler() {
        try {
            Class.forName("org.springframework.remoting.rmi.RmiInvocationHandler");
        } catch (ClassNotFoundException e1) {
            throw new RpcException(
                    "set codec spring for protocol rmi,"
                            + " but NO spring class org.springframework.remoting.rmi.RmiInvocationHandler at provider side!");
        }
    }

    public <T> Remote getProxy(final Invoker<T> invoker) {
        assertRmiInvocationHandler();
        return new org.springframework.remoting.rmi.RmiInvocationHandler() {
            public Object invoke(RemoteInvocation invocation) throws RemoteException,
                    NoSuchMethodException, IllegalAccessException, InvocationTargetException {
                String client = null;
                try {
                    client = RemoteServer.getClientHost();
                } catch (ServerNotActiveException e) {
                    // Ignore it.
                }
                Invocation inv = new RpcInvocation(invocation.getMethodName(),
                        invocation.getParameterTypes(), invocation.getArguments());
                try {
                    RpcContext.getContext().setRemoteAddress(client, 0);
                    return invoker.invoke(inv).recreate();
                } catch (RpcException e) {
                    throw new RemoteException(StringUtils.toString(e));
                } catch (Throwable t) {
                    throw new InvocationTargetException(t);
                }
            }
            public String getTargetInterfaceName() throws RemoteException {
                return invoker.getInterface().getName();
            }
        };
    }

    public <T> Invoker<T> getInvoker(final Remote remote, final Class<T> serviceType, final URL url) {
        assertRmiInvocationHandler();
        return new Invoker<T>() {
            public Class<T> getInterface() {
                return serviceType;
            }

            public URL getUrl() {
                return url;
            }

            public boolean isAvailable() {
                return true;
            }

            public Result invoke(Invocation invocation) throws RpcException {
                RpcResult result = new RpcResult();
                try {
                    RemoteInvocation i = new RemoteInvocation();
                    i.setMethodName(invocation.getMethodName());
                    i.setParameterTypes(invocation.getParameterTypes());
                    i.setArguments(invocation.getArguments());
                    result.setResult(((RmiInvocationHandler) remote).invoke(i));
                } catch (InvocationTargetException e) {
                    result.setException(e.getTargetException());
                } catch (Exception e) {
                    throw new RpcException(StringUtils.toString(e), e);
                }
                return result;
            }

            public void destroy() {
            }
        };
    }

}