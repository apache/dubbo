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
package com.alibaba.dubbo.rpc.cluster.support;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.ExtensionLoader;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.cluster.Directory;
import com.alibaba.dubbo.rpc.cluster.Router;
import com.alibaba.dubbo.rpc.cluster.RouterFactory;

/**
 * 增加router的Directory
 * @author chao.liuc
 */
public abstract class AbstractDirectory<T> implements Directory<T> {
    
    private final URL url ;
    protected volatile boolean destroyed = false;

    private List<Router> routers = new ArrayList<Router>();
    
    public AbstractDirectory(URL url) {
        this(url, null);
    }
    
    public AbstractDirectory(URL url, List<Router> routers) {
        if (url == null)
            throw new IllegalArgumentException("url == null");
        if (routers == null){
            routers = new ArrayList<Router>();
        }
        
        this.url = url;
        String routerkey = url.getParameter(Constants.ROUTER_KEY);
        if (routerkey != null && routerkey.length()>0 ){
            RouterFactory routerFactory = ExtensionLoader.getExtensionLoader(RouterFactory.class).getExtension(routerkey);
            routers.add(routerFactory.getRouter(url));
        }
        if (routers != null) {
            setRouters(routers);
        }
    }
    
    public List<Invoker<T>> list(Invocation invocation) throws RpcException {
        if (destroyed){
            throw new RpcException("Directory already destroyed .url: "+ getUrl());
        }
        List<Invoker<T>> invokers = doList(invocation);
        for (Router router: routers){
            invokers = router.route(invokers, invocation);
        }
        return invokers;
    }
    
    public URL getUrl() {
        return url;
    }
    
    public List<Router> getRouters(){
        return routers;
    }
    
    protected abstract List<Invoker<T>> doList(Invocation invocation) throws RpcException ;
    
    protected void setRouters(List<Router> r){
        routers = r;
    }
    
    public void destroy(){
        destroyed = true;
    }
}