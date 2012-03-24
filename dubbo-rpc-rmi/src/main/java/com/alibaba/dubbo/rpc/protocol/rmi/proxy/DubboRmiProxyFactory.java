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

import java.lang.reflect.Method;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtNewMethod;
import javassist.NotFoundException;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.bytecode.ClassGenerator;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.rmi.RmiProxyFactory;

/**
 * DubboRmiProxyFactory
 * 
 * @author william.liangf
 */
public class DubboRmiProxyFactory implements RmiProxyFactory {
    
    private ProxyFactory proxyFactory;

    public void setProxyFactory(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    public boolean isSupported(Remote remote, Class<?> serviceType, URL url) {
        for (Class<?> i : remote.getClass().getInterfaces()) {
            if (i.getName().endsWith("$Remote")) {
                return true;
            }
        }
        return false;
    }

    public <T> Remote getProxy(final Invoker<T> invoker) {
        final Class<T> remoteClass = getRemoteClass(invoker.getInterface());
        return (Remote) proxyFactory.getProxy(new Invoker<T>() {
            public Class<T> getInterface() {
                return remoteClass;
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

    @SuppressWarnings("unchecked")
    public <T> Invoker<T> getInvoker(Remote remote, Class<T> serviceType, URL url) {
        return proxyFactory.getInvoker((T) remote, serviceType, url);
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T> getRemoteClass(Class<T> type) {
        if (Remote.class.isAssignableFrom(type)) {
            return type;
        }
        try {
            String remoteType = type.getName() + "$Remote";
            try {
                return (Class<T>) Class.forName(remoteType, true, type.getClassLoader());
            } catch (ClassNotFoundException e) {
                ClassPool pool = ClassGenerator.getClassPool(type.getClassLoader());
                CtClass ctClass = pool.makeInterface(remoteType);
                // ctClass.addInterface(pool.getCtClass(type.getName()));
                ctClass.addInterface(pool.getCtClass(Remote.class.getName()));
                Method[] methods = type.getMethods();
                for (Method method : methods) {
                    CtClass[] parameters = new CtClass[method.getParameterTypes().length];
                    int i = 0;
                    for (Class<?> pt : method.getParameterTypes()) {
                        parameters[i++] = pool.getCtClass(pt.getCanonicalName());
                    }
                    CtClass[] exceptions = new CtClass[method.getExceptionTypes().length + 1];
                    exceptions[0] = pool.getCtClass(RemoteException.class.getName());
                    i = 1;
                    for (Class<?> et : method.getExceptionTypes()) {
                        exceptions[i++] = pool.getCtClass(et.getCanonicalName());
                    }
                    ctClass.addMethod(CtNewMethod.abstractMethod(
                            pool.getCtClass(method.getReturnType().getCanonicalName()),
                            method.getName(), parameters, exceptions, ctClass));
                }
                return ctClass.toClass();
            }
        } catch (CannotCompileException e) {
            throw new IllegalStateException(e.getMessage(), e);
        } catch (NotFoundException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

}