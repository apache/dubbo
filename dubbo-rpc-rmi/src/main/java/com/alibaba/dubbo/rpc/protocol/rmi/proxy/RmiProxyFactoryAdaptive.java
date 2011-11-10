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

import java.rmi.Remote;

import com.alibaba.dubbo.common.Adaptive;
import com.alibaba.dubbo.common.ExtensionLoader;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.protocol.rmi.RmiProxyFactory;

/**
 * RmiProxyFactoryAdaptive
 * 
 * @author william.liangf
 */
@Adaptive
public class RmiProxyFactoryAdaptive implements RmiProxyFactory {

    public <T> Remote getProxy(Invoker<T> invoker) {
        ExtensionLoader<RmiProxyFactory> extensionLoader = ExtensionLoader.getExtensionLoader(RmiProxyFactory.class);
        String name = invoker.getUrl().getParameter("codec", "spring");
        if (name != null && name.length() > 0 && ! extensionLoader.hasExtension(name)) {
            throw new IllegalArgumentException("Unsupported protocol codec " + name
                    + " for protocol RMI, Only support: " + extensionLoader.getSupportedExtensions());
        }
        if (Remote.class.isAssignableFrom(invoker.getInterface())) {
            name = "native";
        }
        return extensionLoader.getExtension(name).getProxy(invoker);
    }

    public <T> Invoker<T> getInvoker(Remote remote, Class<T> serviceType, URL url) {
        ExtensionLoader<RmiProxyFactory> extensionLoader = ExtensionLoader.getExtensionLoader(RmiProxyFactory.class);
        for (String name : extensionLoader.getSupportedExtensions()) {
            RmiProxyFactory rmiProxyFactory = extensionLoader.getExtension(name);
            if (rmiProxyFactory.isSupported(remote, serviceType, url)) {
                return rmiProxyFactory.getInvoker(remote, serviceType, url);
            }
        }
        throw new UnsupportedOperationException("Unsupported remote stub " + remote + " by type " + extensionLoader.getSupportedExtensions() + ", service: " + serviceType.getName() + ", url" + url);
    }

    public boolean isSupported(Remote remote, Class<?> serviceType, URL url) {
        return true;
    }

}
