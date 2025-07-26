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
package com.alibaba.dubbo.rpc.protocol;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.ExporterListener;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.InvokerListener;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.listener.ListenerExporterWrapper;
import com.alibaba.dubbo.rpc.listener.ListenerInvokerWrapper;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * ListenerProtocol
 * Protocol的Wrapper拓展实现类，用于给Exporter增加ExporterListener，监听Exporter暴露完成和取消暴露完成
 */
public class ProtocolListenerWrapper implements Protocol {

    private final Protocol protocol;

    public ProtocolListenerWrapper(Protocol protocol) {
        if (protocol == null) {
            throw new IllegalArgumentException("protocol == null");
        }
        this.protocol = protocol;
    }

    public int getDefaultPort() {
        return protocol.getDefaultPort();
    }

    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        //注册中心
        if (Constants.REGISTRY_PROTOCOL.equals(invoker.getUrl().getProtocol())) {
            return protocol.export(invoker);
        }
        //暴露服务，创建Exporter对象
        Exporter<T> export = protocol.export(invoker);
        //获得ExporterListener数组
        List<ExporterListener> exporterListeners = Collections.unmodifiableList(ExtensionLoader.getExtensionLoader(ExporterListener.class)
            .getActivateExtension(invoker.getUrl(), Constants.EXPORTER_LISTENER_KEY));
        //创建带 ExporterListener 的 Exporter 对象
        return new ListenerExporterWrapper<T>(export, exporterListeners);
    }

    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
        //注册中心协议
        if (Constants.REGISTRY_PROTOCOL.equals(url.getProtocol())) {
            return protocol.refer(type, url);
        }
        //引用服务
        Invoker<T> refer = protocol.refer(type, url);
        //获得InvokerListener(监听器)数组
        List<InvokerListener> invokerListeners = Collections.unmodifiableList(
            ExtensionLoader.getExtensionLoader(InvokerListener.class).getActivateExtension(url, Constants.INVOKER_LISTENER_KEY));
        //创建带有InvokerListener的ListenerInvokerWrapper对象
        return new ListenerInvokerWrapper<T>(refer, invokerListeners);
    }

    public void destroy() {
        protocol.destroy();
    }

}