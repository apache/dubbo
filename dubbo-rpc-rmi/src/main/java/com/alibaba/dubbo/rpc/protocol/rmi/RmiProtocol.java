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
package com.alibaba.dubbo.rpc.protocol.rmi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtNewMethod;
import javassist.LoaderClassPath;
import javassist.NotFoundException;

import org.springframework.remoting.rmi.RmiInvocationHandler;
import org.springframework.remoting.support.RemoteInvocation;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.Extension;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.RpcResult;
import com.alibaba.dubbo.rpc.protocol.AbstractProtocol;

/**
 * RmiProtocol.
 * 
 * @author qian.lei
 */
@Extension("rmi")
public class RmiProtocol extends AbstractProtocol {

    public static final int                   DEFAULT_PORT = 1099;

    private final Map<Integer, Registry>      registryMap  = new ConcurrentHashMap<Integer, Registry>();

    private ProxyFactory                      proxyFactory;

    public void setProxyFactory(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    public int getDefaultPort() {
        return DEFAULT_PORT;
    }
    
    @SuppressWarnings("unchecked")
    private <T> Invoker<T> getInvoker(final Remote remote, final Class<T> serviceType, final URL url) {
        final Class<T> remoteType = RmiProtocol.getRemoteClass(serviceType);
        if (ReflectUtils.isInstance(remote, "org.springframework.remoting.rmi.RmiInvocationHandler")) {
            // is the Remote wrap type in spring? (spring rmi is used in Dubbo1)
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
        } else {
            return proxyFactory.getInvoker((T) remote, remoteType, url);
        }
    }

    private <T> Remote getRemote(final Invoker<T> invoker) {
        final Class<T> serviceType = invoker.getInterface();
        final URL url = invoker.getUrl();
        String codec = url.getParameter(Constants.CODEC_KEY, "default");
        if (! "spring".equals(codec) && ! "default".equals(codec) && ! "dubbo".equals(codec) && ! "dubbo2".equals(codec)) {
            throw new IllegalArgumentException("Unsupported protocol codec " + codec
                    + " for protocol RMI, Only support \"default\", \"spring\" codec.");
        }
        if (! Remote.class.isAssignableFrom(serviceType) && "spring".equals(codec)) {
            try {
                Class.forName("org.springframework.remoting.rmi.RmiInvocationHandler");
            } catch (ClassNotFoundException e1) {
                throw new RpcException(
                        "set codec spring for protocol rmi,"
                                + " but NO spring class org.springframework.remoting.rmi.RmiInvocationHandler at provider side!");
            }
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
                    return serviceType.getName();
                }
            };
        } else {
            final Class<T> remoteClass = getRemoteClass(serviceType);
            return (Remote) proxyFactory.getProxy(new Invoker<T>() {
                public Class<T> getInterface() {
                    return remoteClass;
                }

                public URL getUrl() {
                    return url;
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
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Class<T> getRemoteClass(Class<T> type) {
        if (Remote.class.isAssignableFrom(type)) {
            return type;
        }
        try {
            ClassPool pool = new ClassPool(true);
            pool.appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
            CtClass ctClass = pool.makeInterface(type.getName() + "$Remote");
            ctClass.addInterface(pool.getCtClass(type.getName()));
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
        } catch (CannotCompileException e) {
            throw new IllegalStateException(e.getMessage(), e);
        } catch (NotFoundException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        Remote remote = getRemote(invoker);
        // export.
        try {
            UnicastRemoteObject.exportObject(remote, 0);
        } catch (RemoteException e) {
            if ("object already exported".equalsIgnoreCase(e.getMessage())) {
                logger.warn("Ignore 'object already exported' exception.", e);
            } else {
                throw new RpcException("Export rmi service error.", e);
            }
        }
        // register.
        Registry registry = getOrCreateRegistry(invoker.getUrl().getPort());
        try {
            // bind service.
            registry.bind(invoker.getUrl().getPath(), remote);
        } catch (RemoteException e) {
            throw new RpcException("Bind rmi service [" + invoker.getUrl().getPath() + "] error.",
                    e);
        } catch (AlreadyBoundException e) {
            throw new RpcException("Bind rmi service error. Service name ["
                    + invoker.getUrl().getPath() + "] already bound.", e);
        }
        RmiExporter<T> exporter = new RmiExporter<T>(invoker, remote, registry);
        exporterMap.put(serviceKey(invoker.getUrl()), exporter);
        return exporter;
    }

    public <T> Invoker<T> refer(Class<T> serviceType, URL url) throws RpcException {
        Invoker<T> invoker;
        try {
            Registry registry = LocateRegistry.getRegistry(url.getHost(), url.getPort());
            String path = url.getPath();
            if (path == null || path.length() == 0) {
                path = serviceType.getName();
            }
            final Remote rmt = registry.lookup(path);
            invoker = new RmiInvoker<T>(getInvoker(rmt, serviceType, url));
        } catch (RemoteException e) {
            Throwable cause = e.getCause();
            boolean isExportedBySpringButNoSpringClass = ClassNotFoundException.class
                    .isInstance(cause)
                    && cause.getMessage().contains(
                            "org.springframework.remoting.rmi.RmiInvocationHandler");

            String msg = String
                    .format("Can not create remote object%s. url = %s",
                            isExportedBySpringButNoSpringClass ? "(Rmi object is exported by spring rmi but NO spring class org.springframework.remoting.rmi.RmiInvocationHandler at consumer side)"
                                    : "", url);
            throw new RpcException(msg, e);
        } catch (NotBoundException e) {
            throw new RpcException("Rmi service not found. url = " + url, e);
        }
        invokers.add(invoker);
        return invoker;
    }

    protected Registry getOrCreateRegistry(int port) {
        Registry registry = registryMap.get(port);
        if (registry == null) {
            try {
                registry = LocateRegistry.createRegistry(port);
            } catch (RemoteException e) {
                throw new IllegalStateException("Failed to create rmi registry on port " + port
                        + ", cause: " + e.getMessage(), e);
            }
            registryMap.put(port, registry);
        }
        return registry;
    }

    public void destroy() {
        super.destroy();
        for (Integer key : new ArrayList<Integer>(registryMap.keySet())) {
            Registry registry = registryMap.remove(key);
            if (registry != null) {
                try {
                    String[] services = registry.list();
                    if (services != null && services.length > 0) {
                        for (String service : services) {
                            if (logger.isInfoEnabled()) {
                                logger.info("Unbind rmi service: " + service);
                            }
                            registry.unbind(service);
                        }
                    }
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            }
        }
    }

}
