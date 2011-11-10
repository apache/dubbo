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
package com.alibaba.dubbo.rpc.proxy.wrapper;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.Version;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.service.GenericService;

/**
 * MockProxyFactoryWrapper
 * 
 * @author william.liangf
 */
public class MockProxyFactoryWrapper implements ProxyFactory {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MockProxyFactoryWrapper.class);
    
    private final ProxyFactory proxyFactory;
    
    public MockProxyFactoryWrapper(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }
    
    @SuppressWarnings({ "unchecked"})
    public <T> T getProxy(Invoker<T> invoker) throws RpcException {
        String mock = invoker.getUrl().getParameter(Constants.MOCK_KEY);
        if (ConfigUtils.isNotEmpty(mock) && GenericService.class != invoker.getInterface()) {
            Class<?> serviceType = invoker.getInterface();
            if (ConfigUtils.isDefault(mock)) {
                mock = serviceType.getName() + "Mock";
            }
            try {
                Class<?> mockClass = ReflectUtils.forName(mock);
                if (! serviceType.isAssignableFrom(mockClass)) {
                    throw new IllegalArgumentException("The mock implemention class " + mockClass.getName() + " not implement interface " + serviceType.getName());
                }
                try {
                    T mockObject = (T) mockClass.newInstance();
                    invoker = new MockProxyInvoker<T>(invoker, proxyFactory.getInvoker(mockObject, invoker.getInterface(), invoker.getUrl()));
                } catch (InstantiationException e) {
                    throw new IllegalStateException("No such empty constructor \"public " + mockClass.getSimpleName() + "()\" in mock implemention class " + mockClass.getName(), e);
                }
            } catch (Throwable t) {
                LOGGER.error("Failed to create mock implemention class " + mock + " in consumer " + NetUtils.getLocalHost() + " use dubbo version " + Version.getVersion() + ", cause: " + t.getMessage(), t);
                // ignore
            }
        }
        return proxyFactory.getProxy(invoker);
    }
    
    public <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) throws RpcException {
        return proxyFactory.getInvoker(proxy, type, url);
    }
    
}