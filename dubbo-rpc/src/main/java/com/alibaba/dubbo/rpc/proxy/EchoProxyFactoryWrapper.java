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
package com.alibaba.dubbo.rpc.proxy;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.service.EchoService;

/**
 * EchoProxyFactoryWrapper
 * 
 * @author william.liangf
 */
public class EchoProxyFactoryWrapper implements ProxyFactory {
    
    private final ProxyFactory proxyFactory;
    
    public EchoProxyFactoryWrapper(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }
    
    private Class<?>[] appendEchoService(Class<?>[] types) {
        if (types == null || types.length == 0) {
            return new Class<?>[] { EchoService.class };
        } else {
            Class<?>[] clses = new Class<?>[types.length + 1];
            System.arraycopy(types, 0, clses, 0, types.length);
            clses[types.length] = EchoService.class;
            return clses;
        }
    }

    public <T> T getProxy(Invoker<T> invoker, Class<?>... types) throws RpcException {
        return proxyFactory.getProxy(invoker, appendEchoService(types));
    }
    
    public <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) throws RpcException {
        return proxyFactory.getInvoker(proxy, type, url);
    }
    
}