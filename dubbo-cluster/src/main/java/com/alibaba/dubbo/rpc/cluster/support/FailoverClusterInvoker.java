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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.Extension;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.Version;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.cluster.Directory;
import com.alibaba.dubbo.rpc.cluster.LoadBalance;

/**
 * 失败转移，当出现失败，重试其它服务器，通常用于读操作，但重试会带来更长延迟。
 * 
 * <a href="http://en.wikipedia.org/wiki/Failover">Failover</a>
 * @author william.liangf
 *
 */
public class FailoverClusterInvoker<T> extends AbstractClusterInvoker<T>{
    private static final Logger logger = LoggerFactory.getLogger(FailoverClusterInvoker.class);
    
    public FailoverClusterInvoker(Directory<T> directory) {
        super(directory);
    }
    
  public Result doInvoke(Invocation invocation, List<Invoker<T>> invokers, LoadBalance loadbalance) throws RpcException {
        if (invokers == null || invokers.size() == 0)
            throw new RpcException("No provider available for service " + getInterface().getName() + " on consumer " + NetUtils.getLocalHost() + " use dubbo version " + Version.getVersion() + ", Please check whether the service do exist or version is right firstly, and check the provider has started.");

        int len = getUrl().getMethodParameter(invocation.getMethodName(), Constants.RETRIES_KEY, Constants.DEFAULT_RETRIES) + 1;
        if (len <= 0)
            len = 1;

        // retry loop.
        RpcException le = null; // last exception.
        List<Invoker<T>> invoked = new ArrayList<Invoker<T>>(invokers.size()); // invoked invokers.
        Set<URL> providers = new HashSet<URL>(len);
        for (int i = 0; i < len; i++) {
            Invoker<T> invoker = select(loadbalance, invocation, invokers, invoked);
            invoked.add(invoker);
            providers.add(invoker.getUrl());
            try {
                return invoker.invoke(invocation);
            } catch (RpcException e) {
                if (e.isBiz()) throw e;

                le = e;
                logger.warn("" + (i + 1) + "/" + len + " time fail to invoke providers " + providers + " " + loadbalance.getClass().getAnnotation(Extension.class).value()
                        + " select from all providers " + invokers + " for service " + getInterface().getName() + " method " + invocation.getMethodName()
                        + " on consumer " + NetUtils.getLocalHost() + " use dubbo version " + Version.getVersion() + ": " + e.getMessage(), e);
            } catch (Throwable e) // biz exception.
            {
                throw new RpcException(e.getMessage(), e);
            } 
        }
        List<URL> urls = new ArrayList<URL>(invokers.size());
        for(Invoker<T> invoker : invokers){
            if(invoker != null ) 
                urls.add(invoker.getUrl());
        }
        throw new RpcException(le.getCode(),
                "Tried " + len + " times to invoke providers " + providers + " " + loadbalance.getClass().getAnnotation(Extension.class).value()
                + " select from all providers " + invokers + " for service " + getInterface().getName() + " method " + invocation.getMethodName()
                + " on consumer " + NetUtils.getLocalHost() + " use dubbo version " + Version.getVersion()
                + ", but no luck to perform the invocation. Last error is: " + (le != null ? le.getMessage() : ""), le);
    }
}