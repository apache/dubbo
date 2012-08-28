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
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.cluster.Cluster;
import com.alibaba.dubbo.rpc.cluster.Directory;
import com.alibaba.dubbo.rpc.cluster.LoadBalance;

/**
 * AvailableCluster
 * 
 * @author william.liangf
 * @author ding.lid
 */
public class AvailableCluster implements Cluster {
    
    public static final String NAME = "available";

    static <T> boolean isConnectedInvoker(Invoker<T> invoker) {
        return invoker.getUrl().getParameter(Constants.INVOKER_CONNECTED_KEY, true);
    }

    static <T> int getInvokerCount(Invoker<T> invoker) {
        return invoker.getUrl().getParameter(Constants.INVOKER_INSIDE_INVOKER_COUNT_KEY, 1);
    }


    static <T> List<Invoker<T>> getEffectiveInvokers(List<Invoker<T>> invokers) {
        List<Invoker<T>> availableInvokers = new ArrayList<Invoker<T>>();
        List<Invoker<T>> availableAndConnectedInvokers = new ArrayList<Invoker<T>>();

        for (Invoker<T> invoker : invokers) {
            if(invoker.isAvailable()) {
                if (isConnectedInvoker(invoker)) {
                    availableAndConnectedInvokers.add(invoker);
                }
                availableInvokers.add(invoker);
            }
        }

        List<Invoker<T>> effectiveInvokers = availableAndConnectedInvokers;
        if(effectiveInvokers.isEmpty()) effectiveInvokers = availableInvokers; // 都不是connected，则使用available
        return effectiveInvokers;
    }

    static final int WIN_FACTOR = 2;

    /**
     * 优先返回前面的Inovker，除非后面的Invoker的invoker.count值 >= 2倍。
     * TODO Hard Code了因子 2！
     */
    static <T> Invoker<T> getSuitableInvoker(List<Invoker<T>> invokers) {
        if(invokers.isEmpty()) {
            throw new RpcException("No provider available in " + invokers);
        }
        if(invokers.size() == 1) {
            return invokers.get(0);
        }

        int i = 0;
        LOOP_BEFORE:
        for (; i < invokers.size(); i++) {
            Invoker<T> before =  invokers.get(i);
            for (int j = i + 1; j < invokers.size(); j++) {
                Invoker<T> after =  invokers.get(j);
                if(getInvokerCount(before) <= getInvokerCount(after)) {
                    // 被后面的打败了！ 重找
                    continue LOOP_BEFORE;
                }
            }
            break ; // 没有被打败，收工！
        }

        return invokers.get(i);
    }

    public <T> Invoker<T> join(Directory<T> directory) throws RpcException {
        return new AbstractClusterInvoker<T>(directory) {
            public Result doInvoke(Invocation invocation, List<Invoker<T>> invokers, LoadBalance loadbalance) throws RpcException {
                List<Invoker<T>> effectiveInvokers = getEffectiveInvokers(invokers);
                return getSuitableInvoker(effectiveInvokers).invoke(invocation);
            }
        };
    }
}