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
import com.alibaba.dubbo.rpc.filter.tps.DefaultTPSLimiter;
import com.alibaba.dubbo.rpc.filter.tps.TPSLimiter;

/**
 * 限制 service 或方法的 tps.
 *
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
@Activate(group = Constants.PROVIDER, value = Constants.TPS_LIMIT_RATE_KEY)
public class TpsLimitFilter implements Filter {

    private final TPSLimiter tpsLimiter = new DefaultTPSLimiter();

    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {

        if (!tpsLimiter.isAllowable(invoker.getUrl(), invocation)) {
            throw new RpcException(
                    new StringBuilder(64)
                            .append("Failed to invoke service ")
                            .append(invoker.getInterface().getName())
                            .append(".")
                            .append(invocation.getMethodName())
                            .append(" because exceed max service tps.")
                            .toString());
        }

        return invoker.invoke(invocation);
    }

}
