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
package com.alibaba.dubbo.rpc.support;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.ExtensionLoader;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcConstants;
import com.alibaba.dubbo.rpc.RpcException;

/**
 * ListenerProtocol
 * 
 * @author william.liangf
 */
public class ProtocolFilterWrapper implements Protocol {

    private final Protocol protocol;

    public ProtocolFilterWrapper(Protocol protocol){
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
        return protocol.export(buildInvokerChain(invoker, invoker.getUrl().getParameter(Constants.SERVICE_FILTER_KEY), RpcConstants.DEFAULT_SERVICE_FILTERS));
    }

    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
        if (Constants.REGISTRY_PROTOCOL.equals(url.getProtocol())) {
            return protocol.refer(type, url);
        }
        return buildInvokerChain(protocol.refer(type, url), url.getParameter(Constants.REFERENCE_FILTER_KEY), RpcConstants.DEFAULT_REFERENCE_FILTERS);
    }

    public void destroy() {
        protocol.destroy();
    }

    private static <T> Invoker<T> buildInvokerChain(final Invoker<T> invoker, String config, List<String> defaults) {
        List<String> names = ConfigUtils.mergeValues(Filter.class, config, defaults);
        Invoker<T> last = invoker;
        if (names.size() > 0) {
            List<Filter> filters = new ArrayList<Filter>(names.size());
            for (String name : names) {
                filters.add(ExtensionLoader.getExtensionLoader(Filter.class).getExtension(name));
            }
            if (filters.size() > 0) {
                for (int i = filters.size() - 1; i >= 0; i --) {
                    final Filter filter = filters.get(i);
                    final Invoker<T> next = last;
                    last = new Invoker<T>() {

                        public Class<T> getInterface() {
                            return invoker.getInterface();
                        }

                        public URL getUrl() {
                            return invoker.getUrl();
                        }

                        public boolean isAvailable() {
                            return invoker.isAvailable();
                        }

                        public Result invoke(Invocation invocation) throws RpcException {
                            return filter.invoke(next, invocation);
                        }

                        public void destroy() {
                            invoker.destroy();
                        }

                        @Override
                        public String toString() {
                            return invoker.toString();
                        }
                    };
                }
            }
        }
        return last;
    }
    
}