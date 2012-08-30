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
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.cluster.Cluster;
import com.alibaba.dubbo.rpc.cluster.Directory;
import com.alibaba.dubbo.rpc.cluster.LoadBalance;

/**
 * SwitchCluster.
 *
 * 支持在多个Invoker（注册中心Invoker，内部Wrapper了多个Invoker）之间切换。
 * <ol>
 * <li>选用连接着（Connected）的注册中心。《br />
 * 如果注册中心全都不是Connected的，则退化选用用是Available的注册中心(即有Available的Provider)。<br />
 * 这一步，可以选出多个注册中心
 * <li> 优先使用 前面配置的注册中心（即配置注册中心序列表示优先级），除非后面的注册中心的Provider数 >= 此注册中心的Provider * 比例因子。 <br/>
 * 缺省比例因子是<code>2</code>。
 * </ol>
 *
 * 
 * @author ding.lid
 */
public class SwitchCluster implements Cluster {

    private static final Logger logger = LoggerFactory.getLogger(SwitchCluster.class);
    
    public static final String NAME = "switch";

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

    static <T> Invoker<T> getSuitableInvoker(List<Invoker<T>> invokers, Directory<T> directory) {
        if(invokers.isEmpty()) {
            throw new RpcException("No provider available in " + invokers);
        }
        if(invokers.size() == 1) {
            return invokers.get(0);
        }

        final double factor = directory.getUrl().getParameter(
                Constants.CLUSTER_SWITCH_FACTOR, Constants.DEFAULT_CLUSTER_SWITCH_FACTOR);
        int i = 0;
        LOOP_BEFORE:
        for (; i < invokers.size(); i++) {
            Invoker<T> before =  invokers.get(i);
            for (int j = i + 1; j < invokers.size(); j++) {
                Invoker<T> after =  invokers.get(j);
                if(factor * getInvokerCount(before) <= getInvokerCount(after)) {
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
                Invoker<T> invoker = getSuitableInvoker(effectiveInvokers, directory);
                if(invoker != invokers.get(0)) {
                    if(directory.getUrl().getParameter(Constants.CLUSTER_SWITCH_LOG_ERROR, true)) {
                        if(logger.isErrorEnabled())
                            logger.error("SwitchCluster NOT use FIRST invoker " + invokers.get(0) + " of invoker list " + invokers);
                    }
                    else {
                        if(logger.isWarnEnabled())
                            logger.error("SwitchCluster NOT use FIRST invoker " + invokers.get(0) + " of invoker list " + invokers);
                    }
                }
                return invoker.invoke(invocation);
            }
        };
    }
}