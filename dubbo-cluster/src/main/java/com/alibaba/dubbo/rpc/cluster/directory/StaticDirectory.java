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
package com.alibaba.dubbo.rpc.cluster.directory;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.cluster.Router;

/**
 * StaticDirectory
 * 
 * @author william.liangf
 */
public class StaticDirectory<T> extends AbstractDirectory<T> {
    
    private final List<Invoker<T>> invokers;
    
    public StaticDirectory(List<Invoker<T>> invokers){
        this(null, invokers, null);
    }
    
    public StaticDirectory(List<Invoker<T>> invokers, List<Router> routers){
        this(null, invokers, routers);
    }
    
    public StaticDirectory(URL url, List<Invoker<T>> invokers) {
        this(url, invokers, null);
    }

    public StaticDirectory(URL url, List<Invoker<T>> invokers, List<Router> routers) {
        super(url == null && invokers != null && invokers.size() > 0 ? invokers.get(0).getUrl() : url, routers);
        if (invokers == null || invokers.size() == 0)
            throw new IllegalArgumentException("invokers == null");
        this.invokers = invokers;
    }

    public Class<T> getInterface() {
        return invokers.get(0).getInterface();
    }

    public boolean isAvailable() {
        if (isDestroyed()) {
            return false;
        }
        for (Invoker<T> invoker : invokers) {
            if (invoker.isAvailable()) {
                return true;
            }
        }
        return false;
    }

    public void destroy() {
        if(isDestroyed()) {
            return;
        }
        super.destroy();
        for (Invoker<T> invoker : invokers) {
            invoker.destroy();
        }
        invokers.clear();
    }

    @Override
    protected List<Invoker<T>> doList(Invocation invocation) throws RpcException {

        return invokers;
    }

    volatile URL addKeyUrl = null;

    @Override
    public URL getUrl() {
        if(addKeyUrl == null) {
            List<String> invokerUrlString = new ArrayList<String>();
            for(Invoker<T> invoker : invokers) {
                invokerUrlString.add(invoker.getUrl().toString());
            }
            addKeyUrl = super.getUrl().addParameters(
                    Constants.INVOKER_INSIDE_INVOKERS_KEY, URL.encode(CollectionUtils.join(invokerUrlString, ";")),
                    Constants.INVOKER_INSIDE_INVOKER_COUNT_KEY, String.valueOf(invokerUrlString.size()));
        }
        return addKeyUrl;
    }
}