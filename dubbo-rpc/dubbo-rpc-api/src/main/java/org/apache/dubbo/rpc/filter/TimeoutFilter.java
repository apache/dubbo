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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

import java.util.Arrays;

import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;

/**
 * Log any invocation timeout, but don't stop server from running
 */
@Activate(group = CommonConstants.PROVIDER)
public class TimeoutFilter implements Filter, Filter.Listener {

    private static final Logger logger = LoggerFactory.getLogger(TimeoutFilter.class);

    private static final String TIMEOUT_FILTER_START_TIME = "timeout_filter_start_time";

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        invocation.put(TIMEOUT_FILTER_START_TIME, System.currentTimeMillis());
        return invoker.invoke(invocation);
    }

    @Override
    public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {
        Object startTime = invocation.get(TIMEOUT_FILTER_START_TIME);
        if (startTime != null) {
            long elapsed = System.currentTimeMillis() - (Long) startTime;
            if (invoker.getUrl() != null && elapsed > getTimeout(invoker.getUrl(), invocation)) {
                ((AppResponse) appResponse).clear(); // clear response in case of timeout.
                if (logger.isWarnEnabled()) {
                    logger.warn("invoke timed out. method: " + invocation.getMethodName() + " arguments: " + Arrays.toString(invocation.getArguments()) + " , url is " + invoker.getUrl() + ", invoke elapsed " + elapsed + " ms.");
                }
            }
        }
    }

    @Override
    public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {

    }

    private long getTimeout(URL url, Invocation invocation) {
        long timeout = Integer.MAX_VALUE;
        Object genericTimeout = invocation.getObjectAttachment(TIMEOUT_KEY);
        if (genericTimeout != null) {
            try {
                if (genericTimeout instanceof String) {
                    timeout = Long.parseLong((String) genericTimeout);
                } else {
                    timeout = (Long) genericTimeout;
                }
            } catch (Exception e) {
                // ignore
            }
        }
        return timeout;
    }
}
