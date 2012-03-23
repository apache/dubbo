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
package com.alibaba.dubbo.rpc.protocol.dubbo.support;

import java.util.Collection;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.remoting.exchange.ExchangeServer;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.protocol.dubbo.DubboProtocol;

/**
 * TODO Comment of ProtocolUtils
 * 
 * @author william.liangf
 */
public class ProtocolUtils {

    private static Protocol     protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
    public static ProxyFactory proxy    = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

    public static <T> T refer(Class<T> type, String url) {
        return refer(type, URL.valueOf(url));
    }

    public static <T> T refer(Class<T> type, URL url) {
        return proxy.getProxy(protocol.refer(type, url));
    }
    
    public static Invoker<?> referInvoker(Class<?> type, URL url) {
        return (Invoker<?>)protocol.refer(type, url);
    }

    public static <T> Exporter<T> export(T instance, Class<T> type, String url) {
        return export(instance, type, URL.valueOf(url));
    }

    public static <T> Exporter<T> export(T instance, Class<T> type, URL url) {
        return protocol.export(proxy.getInvoker(instance, type, url));
    }

    public static void closeAll() {
        DubboProtocol.getDubboProtocol().destroy();
        Collection<ExchangeServer> servers = DubboProtocol.getDubboProtocol().getServers();
        for (ExchangeServer server : servers) {
            server.close();
        }
    }
}