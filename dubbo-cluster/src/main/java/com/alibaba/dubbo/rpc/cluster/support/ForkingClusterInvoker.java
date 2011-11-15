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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.Version;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcResult;
import com.alibaba.dubbo.rpc.cluster.Directory;
import com.alibaba.dubbo.rpc.cluster.LoadBalance;

/**
 * 并行调用，只要一个成功即返回，通常用于实时性要求较高的操作，但需要浪费更多服务资源。
 * 
 * @author william.liangf
 */
public class ForkingClusterInvoker<T> extends AbstractClusterInvoker<T>{
    
    public ForkingClusterInvoker(Directory<T> directory) {
        super(directory);
    }
    
    private final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("forking-cluster-timer", true)); 
            
    public Result doInvoke(final Invocation invocation, List<Invoker<T>> invokers, LoadBalance loadbalance) throws RpcException {
        if (invokers == null || invokers.size() == 0)
            throw new RpcException("No provider available for service " + getInterface().getName() + " on consumer " + NetUtils.getLocalHost() + " use dubbo version " + Version.getVersion() + ", Please check whether the service do exist or version is right firstly, and check the provider has started.");
        final List<Invoker<T>> selected;
        final int forks = getUrl().getParameter(Constants.FORKS_KEY, Constants.DEFAULT_FORKS);
        final int timeout = getUrl().getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
        if (forks <= 0 || forks >= invokers.size()) {
            selected = invokers;
        } else {
            selected = new ArrayList<Invoker<T>>();
            for (int i = 0; i < forks; i++) {
                //在invoker列表(排除selected)后,如果没有选够,则存在重复循环问题.见select实现.
                Invoker<T> invoker = select(loadbalance, invocation, invokers, selected);
                if(!selected.contains(invoker)){//防止重复添加invoker
                    selected.add(invoker);
                }
            }
        }
        final AtomicInteger count = new AtomicInteger();
        final BlockingQueue<Result> ref = new LinkedBlockingQueue<Result>();
        for (final Invoker<T> invoker : selected) {
            executor.execute(new Runnable() {
                public void run() {
                    try {
                        Result result = invoker.invoke(invocation);
                        ref.offer(result);
                    } catch(Throwable e) {
                        int value = count.incrementAndGet();
                        if (value >= selected.size()) {
                            ref.offer(new RpcResult(e));
                        }
                    }
                }
            });
        }
        try {
            return ref.poll(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RpcException("Failed to forking invoke provider " + selected + ", cause: " + e.getMessage(), e);
        }
    }
}