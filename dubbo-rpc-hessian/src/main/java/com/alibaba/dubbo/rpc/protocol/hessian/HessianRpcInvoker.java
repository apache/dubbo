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
import com.caucho.hessian.client.HessianProxyFactory;

/**
 * hessian rpc invoker.
 * 
 * @author qianlei
 */
public class HessianRpcInvoker<T> extends AbstractInvoker<T> {

    protected static final String HESSIAN_EXCEPTION_PREFIX = HessianException.class.getPackage().getName() + "."; //fix by tony.chenl

    protected Invoker<T>   invoker;

    @SuppressWarnings("unchecked")
    public HessianRpcInvoker(Class<T> serviceType, URL url, ProxyFactory proxyFactory){
        super(serviceType, url);
        int timeout;
        String t = url.getParameter(Constants.TIMEOUT_KEY);
        if (t != null && t.length() > 0) {
            timeout = Integer.parseInt(t);
        } else {
            timeout = Constants.DEFAULT_TIMEOUT;
        }

        java.net.URL httpUrl = url.setProtocol("http").toJavaURL();
        HessianProxyFactory hessianProxyFactory = new HessianProxyFactory();
        hessianProxyFactory.setConnectTimeout(timeout);
        hessianProxyFactory.setReadTimeout(timeout);
        invoker = proxyFactory.getInvoker((T)hessianProxyFactory.create(serviceType, httpUrl, Thread.currentThread().getContextClassLoader()), serviceType, url);
    }

    @Override
    protected Result doInvoke(Invocation invocation) throws Throwable {
        try {
            return invoker.invoke(invocation);
        } catch (RpcException e) {
            throw e;
        } catch (Throwable e) {
            //fix by tony.chenl
            if (e.getClass().getName().startsWith(HESSIAN_EXCEPTION_PREFIX)) {
                throw new RpcException("Failed to invoke remote service: " + getInterface() + ", method: "
                                       + invocation.getMethodName() + ", cause: " + e.getMessage(), e);
            }
            return new RpcResult(e);
        }
    }

}