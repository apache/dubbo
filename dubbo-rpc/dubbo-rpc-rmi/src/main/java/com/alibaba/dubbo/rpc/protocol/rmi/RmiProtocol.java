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
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;

import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.rmi.RmiProxyFactoryBean;
import org.springframework.remoting.rmi.RmiServiceExporter;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.bytecode.ClassGenerator;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcResult;
import com.alibaba.dubbo.rpc.protocol.AbstractProtocol;

/**
 * RmiProtocol.
 * 
 * @author qian.lei
 */
public class RmiProtocol extends AbstractProtocol {

    public static final int              DEFAULT_PORT = 1099;

    private final Map<Integer, Registry> registryMap  = new ConcurrentHashMap<Integer, Registry>();

    private ProxyFactory                 proxyFactory;

    private RmiProxyFactory              rmiProxyFactory;

    public void setProxyFactory(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    public void setRmiProxyFactory(RmiProxyFactory rmiProxyFactory) {
        this.rmiProxyFactory = rmiProxyFactory;
    }

    public int getDefaultPort() {
        return DEFAULT_PORT;
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
                ctClass.addInterface(getCtClass(pool, Remote.class.getName()));
                Method[] methods = type.getMethods();
                for (Method method : methods) {
                    CtClass[] parameters = new CtClass[method.getParameterTypes().length];
                    int i = 0;
                    for (Class<?> pt : method.getParameterTypes()) {
                        parameters[i++] = getCtClass(pool, pt.getCanonicalName());
                    }
                    CtClass[] exceptions = new CtClass[method.getExceptionTypes().length + 1];
                    exceptions[0] = getCtClass(pool, RemoteException.class.getName());
                    i = 1;
                    for (Class<?> et : method.getExceptionTypes()) {
                        exceptions[i++] = getCtClass(pool, et.getCanonicalName());
                    }
                    ctClass.addMethod(CtNewMethod.abstractMethod(
                            getCtClass(pool, method.getReturnType().getCanonicalName()),
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
    
    //fix javassist version problem (getCtClass is since 3.8.5 ,jboss )
    private static CtClass getCtClass(ClassPool pool, String classname) throws NotFoundException{
        if (classname.charAt(0) == '[')
            return Descriptor.toCtClass(classname, pool);
        else
            return pool.get(classname);
    }

    public <T> Exporter<T> export(final Invoker<T> invoker) throws RpcException {
        if (! Remote.class.isAssignableFrom(invoker.getInterface())
                && "spring".equals(invoker.getUrl().getParameter("codec", "spring"))) {
            final RmiServiceExporter rmiServiceExporter = new RmiServiceExporter();
            rmiServiceExporter.setRegistryPort(invoker.getUrl().getPort());
            rmiServiceExporter.setServiceName(invoker.getUrl().getPath());
            rmiServiceExporter.setServiceInterface(invoker.getInterface());
            rmiServiceExporter.setService(proxyFactory.getProxy(invoker));
            try {
                rmiServiceExporter.afterPropertiesSet();
            } catch (RemoteException e) {
                throw new RpcException(e.getMessage(), e);
            }
            Exporter<T> exporter = new Exporter<T>() {
                public Invoker<T> getInvoker() {
                    return invoker;
                }
                public void unexport() {
                    try {
                        rmiServiceExporter.destroy();
                    } catch (Throwable e) {
                        logger.warn(e.getMessage(), e);
                    }
                    try {
                        invoker.destroy();
                    } catch (Throwable e) {
                        logger.warn(e.getMessage(), e);
                    }
                }
            };
            return exporter;
        } else {
            Remote remote = rmiProxyFactory.getProxy(invoker);
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
    }

    public <T> Invoker<T> refer(final Class<T> serviceType, final URL url) throws RpcException {
        if (! Remote.class.isAssignableFrom(serviceType)
                && "spring".equals(url.getParameter("codec", "spring"))) {
            final RmiProxyFactoryBean rmiProxyFactoryBean = new RmiProxyFactoryBean();
            rmiProxyFactoryBean.setServiceUrl(url.toIdentityString());
            rmiProxyFactoryBean.setServiceInterface(serviceType);
            rmiProxyFactoryBean.setCacheStub(true);
            rmiProxyFactoryBean.setLookupStubOnStartup(true);
            rmiProxyFactoryBean.setRefreshStubOnConnectFailure(true);
            rmiProxyFactoryBean.afterPropertiesSet();
            final Object remoteObject = rmiProxyFactoryBean.getObject();
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
                    try {
                        return new RpcResult(remoteObject.getClass().getMethod(invocation.getMethodName(), invocation.getParameterTypes()).invoke(remoteObject, invocation.getArguments()));
                    } catch (InvocationTargetException e) {
                        Throwable t = e.getTargetException();
                        if (t instanceof RemoteAccessException) {
                            t = ((RemoteAccessException)t).getCause();
                        }
                        if (t instanceof RemoteException) {
                            throw RmiInvoker.setRpcExceptionCode(t, new RpcException("Failed to invoke remote service: " + serviceType + ", method: "
                                    + invocation.getMethodName() + ", url: " + url + ", cause: " + t.getMessage(), t));
                        } else {
                            return new RpcResult(t);
                        }
                    } catch (Throwable e) {
                        if (e instanceof RemoteAccessException) {
                            e = ((RemoteAccessException)e).getCause();
                        }
                        throw RmiInvoker.setRpcExceptionCode(e, new RpcException("Failed to invoke remote service: " + serviceType + ", method: "
                                + invocation.getMethodName() + ", url: " + url + ", cause: " + e.getMessage(), e));
                    }
                }
                public void destroy() {
                }
            };
        } else {
            Invoker<T> invoker;
            try {
                if ("dubbo".equals(url.getParameter("codec"))) {
                    RmiProtocol.getRemoteClass(serviceType);
                }
                Registry registry = LocateRegistry.getRegistry(url.getHost(), url.getPort());
                String path = url.getPath();
                if (path == null || path.length() == 0) {
                    path = serviceType.getName();
                }
                invoker = new RmiInvoker<T>(registry, rmiProxyFactory, rmiProxyFactory.getInvoker(registry.lookup(path), serviceType, url));
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