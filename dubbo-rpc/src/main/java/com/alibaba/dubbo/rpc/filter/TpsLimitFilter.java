/*
 * Copyright 1999-2012 Alibaba Group.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.alibaba.dubbo.rpc.filter;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcStatus;

/**
 * 限制 service 或方法的 tps.
 *
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
@Activate(group = Constants.PROVIDER, value = Constants.TPS_MAX_KEY)
public class TpsLimitFilter implements Filter {

// TODO 现在依赖 ActiveLimitFilter 或 ExecuteLimitFilter 的计数,需要放到这两个 filter 后执行

    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {

        long max = invoker.getUrl().getMethodParameter(
                invocation.getMethodName(), Constants.TPS_MAX_KEY, 0L);
        // verify method tps
        if (max > 0L
                && max <= RpcStatus.getStatus(invoker.getUrl(), invocation.getMethodName()).getAverageTps()) {
            throw new RpcException(
                    new StringBuilder(64)
                            .append("Failed to invoke service ")
                            .append(invoker.getInterface().getName())
                            .append(".")
                            .append(invocation.getMethodName())
                            .append(" because exceed max service tps ")
                            .append(max).toString());
        }

        return invoker.invoke(invocation);
    }

}
