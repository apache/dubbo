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
package com.alibaba.dubbo.rpc.protocol.hessian;

import java.net.SocketTimeoutException;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcResult;
import com.alibaba.dubbo.rpc.protocol.AbstractInvoker;
import com.caucho.hessian.HessianException;
import com.caucho.hessian.client.HessianConnectionException;
import com.caucho.hessian.client.HessianConnectionFactory;
import com.caucho.hessian.client.HessianProxyFactory;
import com.caucho.hessian.io.HessianMethodSerializationException;

/**
 * hessian rpc invoker.
 * 
 * @author qianlei
 */
public class HessianRpcInvoker<T> extends AbstractInvoker<T> {

    protected static final String HESSIAN_EXCEPTION_PREFIX = HessianException.class.getPackage().getName() + "."; //fix by tony.chenl

    protected Invoker<T>   invoker;
    
    protected HessianConnectionFactory hessianConnectionFactory = new HttpClientConnectionFactory();

    @SuppressWarnings("unchecked")
    public HessianRpcInvoker(Class<T> serviceType, URL url, ProxyFactory proxyFactory){
        super(serviceType, url);
        HessianProxyFactory hessianProxyFactory = new HessianProxyFactory();
        String client = url.getParameter(Constants.CLIENT_KEY, Constants.DEFAULT_HTTP_CLIENT);
        if ("httpclient".equals(client)) {
            hessianProxyFactory.setConnectionFactory(hessianConnectionFactory);
        } else if (client != null && client.length() > 0 && ! Constants.DEFAULT_HTTP_CLIENT.equals(client)) {
            throw new IllegalStateException("Unsupported http protocol client=\"" + client + "\"!");
        }
        int timeout = url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
        hessianProxyFactory.setConnectTimeout(timeout);
        hessianProxyFactory.setReadTimeout(timeout);
        invoker = proxyFactory.getInvoker((T)hessianProxyFactory.create(serviceType, url.setProtocol("http").toJavaURL(), Thread.currentThread().getContextClassLoader()), serviceType, url);
    }

    @Override
    protected Result doInvoke(Invocation invocation) throws Throwable {
        try {
            Result result = invoker.invoke(invocation);
            Throwable e = result.getException();
            if (e != null) {
                String name = e.getClass().getName();
                if (name.startsWith(HESSIAN_EXCEPTION_PREFIX)) {
                    RpcException re = new RpcException("Failed to invoke remote service: " + getInterface() + ", method: "
                            + invocation.getMethodName() + ", cause: " + e.getMessage(), e);
                    throw setRpcExceptionCode(e, re);
                }
            }
            return result;
        } catch (RpcException e) {
            throw setRpcExceptionCode(e.getCause(), e);
        } catch (HessianException e) {
            throw setRpcExceptionCode(e, new RpcException("Failed to invoke remote service: " + getInterface() + ", method: "
                    + invocation.getMethodName() + ", cause: " + e.getMessage(), e));
        } catch (Throwable e) {
            return new RpcResult(e);
        }
    }
    
    private RpcException setRpcExceptionCode(Throwable e, RpcException re) {
        if (e != null) {
            if (e instanceof HessianConnectionException) {
                re.setCode(RpcException.NETWORK_EXCEPTION);
                if (e.getCause() != null) {
                    Class<?> cls = e.getCause().getClass();
                    if (SocketTimeoutException.class.equals(cls)) {
                        re.setCode(RpcException.TIMEOUT_EXCEPTION);
                    }
                }
            } else if (e instanceof HessianMethodSerializationException) {
                re.setCode(RpcException.SERIALIZATION_EXCEPTION);
            }
        }
        return re;
    }

}