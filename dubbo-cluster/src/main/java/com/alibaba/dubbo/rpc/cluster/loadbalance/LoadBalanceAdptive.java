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
package com.alibaba.dubbo.rpc.cluster.loadbalance;

import java.util.List;

import com.alibaba.dubbo.common.Adaptive;
import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.ExtensionLoader;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.cluster.LoadBalance;

/**
 * LoadBalanceAdptive
 * 
 * @author ding.lid
 * @author william.liangf
 */
@Adaptive
public class LoadBalanceAdptive implements LoadBalance {
    public <T> Invoker<T> select(List<Invoker<T>> invokers, Invocation invocation) throws RpcException {
        if (invokers == null || invokers.size() == 0) {
            return null;
        }
        URL url = invokers.get(0).getUrl();
        String method = invocation.getMethodName();
        String name;
        if (method == null || method.length() == 0) {
            name = url.getParameter(Constants.LOADBALANCE_KEY, Constants.DEFAULT_LOADBALANCE);
        } else {
            name = url.getMethodParameter(method, Constants.LOADBALANCE_KEY, Constants.DEFAULT_LOADBALANCE);
        }
        LoadBalance loadbalance = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(name);
        return loadbalance.select(invokers, invocation);
    }
}