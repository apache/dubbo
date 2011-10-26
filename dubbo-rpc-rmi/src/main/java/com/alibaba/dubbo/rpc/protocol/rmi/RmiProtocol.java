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

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.AlreadyBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.remoting.support.RemoteInvocation;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.Extension;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.bytecode.Proxy;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.protocol.AbstractProtocol;

/**
 * RmiProtocolSupport.
 * 
 * @author qian.lei
 */
@Extension("rmi")
public class RmiProtocol extends AbstractProtocol {

    public static final int                   DEFAULT_PORT        = 1099;

    private final Map<Integer, Registry>      registryMap        = new ConcurrentHashMap<Integer, Registry>();

    private final Map<String, RmiExporter<?>> exporterMap = new ConcurrentHashMap<String, RmiExporter<?>>(); // <service

    public int getDefaultPort() {
        return DEFAULT_PORT;
    }

    Remote getObjectToExport(final RmiExporter<?> rpcExporter, boolean isSpringCodec, final String host,
                             final int port, final Class<?> serviceType) {
        boolean isRemoteType = Remote.class.isAssignableFrom(serviceType);
        Remote exportedObj;
        if (isRemoteType) {
            Proxy proxy = Proxy.getProxy(new Class<?>[] { serviceType });
            Object proxyService = proxy.newInstance(new InvocationHandler() {

                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    String client = null;
                    try {
                        client = RemoteServer.getClientHost();
                    } catch (ServerNotActiveException e) {
                        // Ignore it.
                    }
                    Invocation inv = new RpcInvocation(method, args);
                    try {
                        return rpcExporter.invoke(inv, client, 0).recreate();
                    } catch (RpcException e) {
                        throw new RemoteException(StringUtils.toString(e));
                    }
                }
            });
            exportedObj = (Remote) proxyService;
        } else if (isSpringCodec) {
            try {
                Class.forName("org.springframework.remoting.rmi.RmiInvocationHandler");
            } catch (ClassNotFoundException e1) {
                throw new RpcException(
                                       "set codec spring for protocol rmi,"
                                               + " but NO spring class org.springframework.remoting.rmi.RmiInvocationHandler at provider side!");
            }
            exportedObj = new org.springframework.remoting.rmi.RmiInvocationHandler() {

                public Object invoke(RemoteInvocation invocation) throws RemoteException, NoSuchMethodException,
                                                                 IllegalAccessException, InvocationTargetException {
                    String client = null;
                    try {
                        client = RemoteServer.getClientHost();
                    } catch (ServerNotActiveException e) {
                        // Ignore it.
                    }

                    Invocation inv = new RpcInvocation(invocation.getMethodName(),
                                                                 invocation.getParameterTypes(),
                                                                 invocation.getArguments());
                    try {
                        return rpcExporter.invoke(inv, client, 0).recreate();
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
            exportedObj = new AbstractRmiInvocationHandler() {

                public Result invoke(Invocation inv) throws RemoteException, NoSuchMethodException,
                                                          IllegalAccessException, InvocationTargetException {
                    String client = null;
                    try {
                        client = RemoteServer.getClientHost();
                    } catch (ServerNotActiveException e) {
                        // Ignore it.
                    }

                    return rpcExporter.invoke(inv, client, 0);
                }
            };
        }
        return exportedObj;
    }

    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        Class<T> serviceType = invoker.getInterface();
        URL url = invoker.getUrl();

        String codec = url.getParameter(Constants.CODEC_KEY, "spring");
        boolean isSpringCodec;
        if ("spring".equals(codec)) {
            isSpringCodec = true;
        } else if ("dubbo".equals(codec)) {
            isSpringCodec = false;
        } else {
            throw new IllegalArgumentException("Unsupported protocol codec " + codec
                    + " for protocol RMI, Only support \"dubbo\", \"spring\" codec.");
        }

        final RmiExporter<T> ret = new RmiExporter<T>(invoker);
        Remote exportedObj = getObjectToExport(ret, isSpringCodec, url.getHost(), url.getPort(), serviceType);

        // export.
        try {
            // UnicastRemoteObject.exportObject(exportedObj, 0, new InternalClientSocketFactory(), new
            // InternalServerSocketFactory());
            UnicastRemoteObject.exportObject(exportedObj, 0);
            ret.setRemoteObject(exportedObj);
        } catch (RemoteException e) {
            if ("object already exported".equalsIgnoreCase(e.getMessage())) logger.warn("Ignore 'object already exported' exception.",
                                                                                        e);
            else throw new RpcException("Export rmi service error.", e);
        }

        // register.
        Registry reg = getOrCreateRegistry(url.getPort());
        try {
            // bind service.
            reg.bind(url.getPath(), exportedObj);
            ret.setRmiRegistry(reg);
        } catch (RemoteException e) {
            throw new RpcException("Bind rmi service [" + url.getPath() + "] error.", e);
        } catch (AlreadyBoundException e) {
            throw new RpcException("Bind rmi service error. Service name [" + url.getPath() + "] already bound.", e);
        }

        exporterMap.put(serviceKey(url), ret);
        return ret;
    }

    public <T> Invoker<T> refer(Class<T> serviceType, URL url) throws RpcException {
        Invoker<T> invoker = new RmiInvoker<T>(serviceType, url);
        invokers.add(invoker);
        return invoker ;
    }

    protected Registry getOrCreateRegistry(int port) {
        Registry reg = registryMap.get(port);
        if (reg == null) {
            try {
                // reg = LocateRegistry.createRegistry(port, new InternalClientSocketFactory(), new
                // InternalServerSocketFactory());
                reg = LocateRegistry.createRegistry(port);
            } catch (RemoteException e) {
                throw new IllegalStateException("Failed to create rmi registry on port " + port + ", cause: " + e.getMessage(), e);
            }
            registryMap.put(port, reg);
        }
        return reg;
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

    /**
     * 自定义的RMIClientSocketFactory类，用于控制Rmi Client端连接的控制，如连接池、监控等。 这个类会序列化传到Rmi Client（Client可能没有这个类），考虑到与Native
     * Rmi的目前不使用这个类 。
     * 
     * @author qian.lei
     */
    @SuppressWarnings("unused")
    private static class InternalClientSocketFactory implements RMIClientSocketFactory, Serializable {

        private static final long serialVersionUID = 8412843862275448994L;

        public Socket createSocket(String host, int port) throws IOException {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(host, port));
            return socket;
        }
    }

    /**
     * @author qian.lei
     */
    @SuppressWarnings("unused")
    private static class InternalServerSocketFactory implements RMIServerSocketFactory {

        public ServerSocket createServerSocket(int port) throws IOException {
            return new InternalServerSocket(port);
        }
    }

    private static class InternalServerSocket extends ServerSocket {

        public InternalServerSocket(int port) throws IOException{
            super(port);
        }

        public Socket accept() throws IOException {
            Socket socket = super.accept();
            return socket;
        }
    }
}