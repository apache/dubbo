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
package com.alibaba.dubbo.rpc.protocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.ExtensionLoader;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.ExporterListener;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.InvokerListener;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.RpcConstants;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.listener.ListenerExporterWrapper;
import com.alibaba.dubbo.rpc.listener.ListenerInvokerWrapper;

/**
 * ListenerProtocol
 * 
 * @author william.liangf
 */
public class ProtocolListenerWrapper implements Protocol {

    private final Protocol protocol;

    public ProtocolListenerWrapper(Protocol protocol){
        if (protocol == null) {
            throw new IllegalArgumentException("protocol == null");
        }
        this.protocol = protocol;
    }

    public int getDefaultPort() {
        return protocol.getDefaultPort();
    }

    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        if (Constants.REGISTRY_PROTOCOL.equals(invoker.getUrl().getProtocol())) {
            return protocol.export(invoker);
        }
        return new ListenerExporterWrapper<T>(protocol.export(invoker), 
                buildServiceListeners(invoker.getUrl().getParameter(Constants.EXPORTER_LISTENER_KEY), RpcConstants.DEFAULT_EXPORTER_LISTENERS));
    }

    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
        if (Constants.REGISTRY_PROTOCOL.equals(url.getProtocol())) {
            return protocol.refer(type, url);
        }
        return new ListenerInvokerWrapper<T>(protocol.refer(type, url), 
                buildReferenceListeners(url.getParameter(Constants.INVOKER_LISTENER_KEY), RpcConstants.DEFAULT_INVOKER_LISTENERS));
    }

    public void destroy() {
        protocol.destroy();
    }
    
    private static List<ExporterListener> buildServiceListeners(String config, List<String> defaults) {
        List<String> names = ConfigUtils.mergeValues(Filter.class, config, defaults);
        List<ExporterListener> listeners = new ArrayList<ExporterListener>();
        if (names.size() > 0) {
            for (String name : names) {
                listeners.add(ExtensionLoader.getExtensionLoader(ExporterListener.class).getExtension(name));
            }
        }
        return Collections.unmodifiableList(listeners);
    }
    
    private static List<InvokerListener> buildReferenceListeners(String config, List<String> defaults) {
        List<String> names = ConfigUtils.mergeValues(Filter.class, config, defaults);
        List<InvokerListener> listeners = new ArrayList<InvokerListener>();
        if (names.size() > 0) {
            for (String name : names) {
                listeners.add(ExtensionLoader.getExtensionLoader(InvokerListener.class).getExtension(name));
            }
        }
        return Collections.unmodifiableList(listeners);
    }

}