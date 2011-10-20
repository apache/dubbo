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

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.Extension;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcStatus;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Result;

/**
 * ThreadLimitInvokerFilter
 * 
 * @author william.liangf
 */
@Extension("executelimit")
public class ExecuteLimitFilter implements Filter {

    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        URL url = invoker.getUrl();
        String methodName = invocation.getMethodName();
        int max = url.getMethodIntParameter(methodName, Constants.EXECUTES_KEY);
        if (max > 0) {
            RpcStatus count = RpcStatus.getStatus(url, invocation.getMethodName());
            if (count.getActive() >= max) {
                throw new RpcException("Failed to invoke invocation " + invocation + " in provider " + url + ", cause: The service using threads greater than <dubbo:service threads=\"" + max + "\" /> limited.");
            }
        }
        long begin = System.currentTimeMillis();
        RpcStatus.beginCount(url, methodName);
        try {
            Result result = invoker.invoke(invocation);
            RpcStatus.endCount(url, methodName, System.currentTimeMillis() - begin, true);
            return result;
        } catch (RuntimeException t) {
            RpcStatus.endCount(url, methodName, System.currentTimeMillis() - begin, false);
            throw t;
        }
    }

}