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
package com.alibaba.dubbo.rpc.filter;

import com.alibaba.dubbo.common.Extension;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcStatus;

/**
 * ReadyFilter
 * 
 * @author william.liangf
 */
@Extension("ready")
public class ReadyFilter implements Filter {

    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (! RpcStatus.getStatus(invoker.getUrl()).isReady()) {
            throw new RpcException("The service not ready! Limit by config <dubbo:service delay=\"...\" />, service: " + invoker.getInterface().getName() + ", url: " + invoker.getUrl());
        }
        if (! RpcStatus.getStatus(invoker.getUrl(), invocation.getMethodName()).isReady()) {
            throw new RpcException("The service method not ready! Limit by config <dubbo:service><dubbo:method delay=\"...\" /></dubbo:service>, service: " + invoker.getInterface().getName() + ", url: " + invoker.getUrl() + ", method: " + invocation.getMethodName());
        }
        return invoker.invoke(invocation);
    }

}