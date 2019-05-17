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
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.ListenableFilter;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcStatus;

import static org.apache.dubbo.rpc.Constants.EXECUTES_KEY;


/**
 *
 * The maximum parallel execution request count per method per service for the provider.If the max configured
 * <b>executes</b> is set to 10 and if invoke request where it is already 10 then it will throws exception. It
 * continue the same behaviour un till it is <10.
 *
 */
@Activate(group = CommonConstants.PROVIDER, value = EXECUTES_KEY)
public class ExecuteLimitFilter extends ListenableFilter {

    private static final String EXECUTELIMIT_FILTER_START_TIME = "execugtelimit_filter_start_time";

    public ExecuteLimitFilter() {
        super.listener = new ExecuteLimitListener();
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        URL url = invoker.getUrl();
        String methodName = invocation.getMethodName();
        int max = url.getMethodParameter(methodName, EXECUTES_KEY, 0);
        if (!RpcStatus.beginCount(url, methodName, max)) {
            throw new RpcException("Failed to invoke method " + invocation.getMethodName() + " in provider " +
                    url + ", cause: The service using threads greater than <dubbo:service executes=\"" + max +
                    "\" /> limited.");
        }

        invocation.setAttachment(EXECUTELIMIT_FILTER_START_TIME, String.valueOf(System.currentTimeMillis()));
        try {
            return invoker.invoke(invocation);
        } catch (Throwable t) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else {
                throw new RpcException("unexpected exception when ExecuteLimitFilter", t);
            }
        }
    }

    static class ExecuteLimitListener implements Listener {
        @Override
        public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {
            RpcStatus.endCount(invoker.getUrl(), invocation.getMethodName(), getElapsed(invocation), true);
        }

        @Override
        public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {
            RpcStatus.endCount(invoker.getUrl(), invocation.getMethodName(), getElapsed(invocation), false);
        }

        private long getElapsed(Invocation invocation) {
            String beginTime = invocation.getAttachment(EXECUTELIMIT_FILTER_START_TIME);
            return StringUtils.isNotEmpty(beginTime) ? System.currentTimeMillis() - Long.parseLong(beginTime) : 0;
        }
    }
}
