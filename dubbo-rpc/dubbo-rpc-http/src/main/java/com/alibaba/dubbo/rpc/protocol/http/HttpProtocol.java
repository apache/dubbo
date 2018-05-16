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
package com.alibaba.dubbo.rpc.protocol.http;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.beanutil.JavaBeanAccessor;
import com.alibaba.dubbo.common.beanutil.JavaBeanDescriptor;
import com.alibaba.dubbo.common.beanutil.JavaBeanSerializeUtil;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.io.UnsafeByteArrayInputStream;
import com.alibaba.dubbo.common.io.UnsafeByteArrayOutputStream;
import com.alibaba.dubbo.common.serialize.Serialization;
import com.alibaba.dubbo.common.utils.PojoUtils;
import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.remoting.http.HttpBinder;
import com.alibaba.dubbo.remoting.http.HttpHandler;
import com.alibaba.dubbo.remoting.http.HttpServer;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.AbstractProxyProtocol;

import com.alibaba.dubbo.rpc.support.ProtocolUtils;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.httpinvoker.HttpComponentsHttpInvokerRequestExecutor;
import org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean;
import org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter;
import org.springframework.remoting.httpinvoker.SimpleHttpInvokerRequestExecutor;
import org.springframework.remoting.support.RemoteInvocation;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HttpProtocol
 */
public class HttpProtocol extends AbstractProxyProtocol {

    public static final int DEFAULT_PORT = 80;

    private final Map<String, HttpServer> serverMap = new ConcurrentHashMap<String, HttpServer>();

    private final Map<String, HttpInvokerServiceExporter> skeletonMap = new ConcurrentHashMap<String, HttpInvokerServiceExporter>();

    private HttpBinder httpBinder;

    public HttpProtocol() {
        super(RemoteAccessException.class);
    }

    public void setHttpBinder(HttpBinder httpBinder) {
        this.httpBinder = httpBinder;
    }

    @Override
    public int getDefaultPort() {
        return DEFAULT_PORT;
    }

    @Override
    protected <T> Runnable doExport(final T impl, Class<T> type, URL url) throws RpcException {
        String addr = getAddr(url);
        HttpServer server = serverMap.get(addr);
        if (server == null) {
            server = httpBinder.bind(url, new InternalHandler());
            serverMap.put(addr, server);
        }
        final HttpInvokerServiceExporter httpServiceExporter = new HttpInvokerServiceExporter() {
            @Override
            protected Object invoke(RemoteInvocation invocation, Object targetObject) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
                if (invocation.getMethodName().equals(Constants.$INVOKE)
                        && invocation.getArguments() != null
                        && invocation.getArguments().length == 3) {
                    String name = ((String) invocation.getArguments()[0]).trim();
                    String[] types = (String[]) invocation.getArguments()[1];
                    Object[] args = (Object[]) invocation.getArguments()[2];

                    Class<?>[] params;
                    try {
                        Method method = ReflectUtils.findMethodByMethodSignature(this.getServiceInterface(), name, types);
                        params = method.getParameterTypes();
                        if (args == null) {
                            args = new Object[params.length];
                        }

                        String generic = (String) invocation.getAttribute(Constants.GENERIC_KEY);
                        if (StringUtils.isEmpty(generic)
                                || ProtocolUtils.isDefaultGenericSerialization(generic)) {
                            args = PojoUtils.realize(args, params, method.getGenericParameterTypes());
                        } else if (ProtocolUtils.isJavaGenericSerialization(generic)) {
                            for (int i = 0; i < args.length; i++) {
                                if (byte[].class == args[i].getClass()) {
                                    try {
                                        UnsafeByteArrayInputStream is = new UnsafeByteArrayInputStream((byte[]) args[i]);
                                        args[i] = ExtensionLoader.getExtensionLoader(Serialization.class)
                                                .getExtension(Constants.GENERIC_SERIALIZATION_NATIVE_JAVA)
                                                .deserialize(null, is).readObject();
                                    } catch (Exception e) {
                                        throw new RpcException("Deserialize argument [" + (i + 1) + "] failed.", e);
                                    }
                                } else {
                                    throw new RpcException(
                                            "Generic serialization [" + generic + "] only support message type " +
                                                    byte[].class + " and your message type is " +
                                                    args[i].getClass());
                                }
                            }
                        } else if (ProtocolUtils.isBeanGenericSerialization(generic)) {
                            for (int i = 0; i < args.length; i++) {
                                if (args[i] instanceof JavaBeanDescriptor) {
                                    args[i] = JavaBeanSerializeUtil.deserialize((JavaBeanDescriptor) args[i]);
                                } else {
                                    throw new RpcException(
                                            "Generic serialization [" + generic + "] only support message type " +
                                                    JavaBeanDescriptor.class.getName() + " and your message type is " +
                                                    args[i].getClass().getName());
                                }
                            }
                        }

                        RemoteInvocation invocation2 = invocation;
                        invocation2.setMethodName(name);
                        invocation2.setParameterTypes(params);
                        invocation2.setArguments(args);

                        Object result = super.invoke(invocation, targetObject);
                        if (ProtocolUtils.isJavaGenericSerialization(generic)) {
                            try {
                                UnsafeByteArrayOutputStream os = new UnsafeByteArrayOutputStream(512);
                                ExtensionLoader.getExtensionLoader(Serialization.class)
                                        .getExtension(Constants.GENERIC_SERIALIZATION_NATIVE_JAVA)
                                        .serialize(null, os).writeObject(result);
                                return os.toByteArray();
                            } catch (IOException e) {
                                throw new RpcException("Serialize result failed.", e);
                            }
                        } else if (ProtocolUtils.isBeanGenericSerialization(generic)) {
                            return JavaBeanSerializeUtil.serialize(result, JavaBeanAccessor.METHOD);
                        } else {
                            return PojoUtils.generalize(result);
                        }
                    } catch (NoSuchMethodException e) {
                        throw new RpcException(e);
                    } catch (Exception e) {
                        throw new RpcException(e);
                    }
                }
                return super.invoke(invocation, targetObject);
            }
        };
        httpServiceExporter.setServiceInterface(type);
        httpServiceExporter.setService(impl);
        try {
            httpServiceExporter.afterPropertiesSet();
        } catch (Exception e) {
            throw new RpcException(e.getMessage(), e);
        }
        final String path = url.getAbsolutePath();
        skeletonMap.put(path, httpServiceExporter);
        return new Runnable() {
            @Override
            public void run() {
                skeletonMap.remove(path);
            }
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T doRefer(final Class<T> serviceType, final URL url) throws RpcException {
        final HttpInvokerProxyFactoryBean httpProxyFactoryBean = new HttpInvokerProxyFactoryBean() {
            @Override
            protected RemoteInvocation createRemoteInvocation(MethodInvocation methodInvocation) {
                RemoteInvocation result = super.createRemoteInvocation(methodInvocation);
                for (Map.Entry<String, String> entry : url.getParameters().entrySet()) {
                    if (!StringUtils.isBlank(entry.getValue())) {
                        result.addAttribute(entry.getKey(), entry.getValue());
                    }
                }
                return result;
            }
        };
        httpProxyFactoryBean.setServiceUrl(url.toIdentityString());
        httpProxyFactoryBean.setServiceInterface(serviceType);
        String client = url.getParameter(Constants.CLIENT_KEY);
        if (client == null || client.length() == 0 || "simple".equals(client)) {
            SimpleHttpInvokerRequestExecutor httpInvokerRequestExecutor = new SimpleHttpInvokerRequestExecutor() {
                @Override
                protected void prepareConnection(HttpURLConnection con,
                                                 int contentLength) throws IOException {
                    super.prepareConnection(con, contentLength);
                    con.setReadTimeout(url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT));
                    con.setConnectTimeout(url.getParameter(Constants.CONNECT_TIMEOUT_KEY, Constants.DEFAULT_CONNECT_TIMEOUT));
                }
            };
            httpProxyFactoryBean.setHttpInvokerRequestExecutor(httpInvokerRequestExecutor);
        } else if ("commons".equals(client)) {
            HttpComponentsHttpInvokerRequestExecutor httpInvokerRequestExecutor = new HttpComponentsHttpInvokerRequestExecutor();
            httpInvokerRequestExecutor.setReadTimeout(url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT));
            httpInvokerRequestExecutor.setConnectTimeout(url.getParameter(Constants.CONNECT_TIMEOUT_KEY, Constants.DEFAULT_CONNECT_TIMEOUT));
            httpProxyFactoryBean.setHttpInvokerRequestExecutor(httpInvokerRequestExecutor);
        } else {
            throw new IllegalStateException("Unsupported http protocol client " + client + ", only supported: simple, commons");
        }
        httpProxyFactoryBean.afterPropertiesSet();
        return (T) httpProxyFactoryBean.getObject();
    }

    @Override
    protected int getErrorCode(Throwable e) {
        if (e instanceof RemoteAccessException) {
            e = e.getCause();
        }
        if (e != null) {
            Class<?> cls = e.getClass();
            if (SocketTimeoutException.class.equals(cls)) {
                return RpcException.TIMEOUT_EXCEPTION;
            } else if (IOException.class.isAssignableFrom(cls)) {
                return RpcException.NETWORK_EXCEPTION;
            } else if (ClassNotFoundException.class.isAssignableFrom(cls)) {
                return RpcException.SERIALIZATION_EXCEPTION;
            }
        }
        return super.getErrorCode(e);
    }

    private class InternalHandler implements HttpHandler {

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response)
                throws IOException, ServletException {
            String uri = request.getRequestURI();
            HttpInvokerServiceExporter skeleton = skeletonMap.get(uri);
            if (!request.getMethod().equalsIgnoreCase("POST")) {
                response.setStatus(500);
            } else {
                RpcContext.getContext().setRemoteAddress(request.getRemoteAddr(), request.getRemotePort());
                try {
                    skeleton.handleRequest(request, response);
                } catch (Throwable e) {
                    throw new ServletException(e);
                }
            }
        }

    }

}
