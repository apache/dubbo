/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.rpc.filter;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;

import java.util.Arrays;

/**
 * Log any invocation timeout, but don't stop server from running
 */
@Activate(group = CommonConstants.PROVIDER)
public class TimeoutFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(TimeoutFilter.class);

    private static final String TIMEOUT_FILTER_START_TIME = "timeout_filter_start_time";

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (invocation.getAttachments() != null) {
            long start = System.currentTimeMillis();
            invocation.getAttachments().put(TIMEOUT_FILTER_START_TIME, String.valueOf(start));
        } else {
            if (invocation instanceof RpcInvocation) {
                RpcInvocation invc = (RpcInvocation) invocation;
                long start = System.currentTimeMillis();
                invc.setAttachment(TIMEOUT_FILTER_START_TIME, String.valueOf(start));
            }
        }
        return invoker.invoke(invocation);
    }

    @Override
    public Result onResponse(Result result, Invoker<?> invoker, Invocation invocation) {
        String startAttach = invocation.getAttachment(TIMEOUT_FILTER_START_TIME);
        if (startAttach != null) {
            long elapsed = System.currentTimeMillis() - Long.valueOf(startAttach);
            if (invoker.getUrl() != null
                    && elapsed > invoker.getUrl().getMethodParameter(invocation.getMethodName(),
                    "timeout", Integer.MAX_VALUE)) {
                if (logger.isWarnEnabled()) {
                    logger.warn("invoke time out. method: " + invocation.getMethodName()
                            + " arguments: " + Arrays.toString(invocation.getArguments()) + " , url is "
                            + invoker.getUrl() + ", invoke elapsed " + elapsed + " ms.");
                }
            }
        }
        return result;
    }
}
