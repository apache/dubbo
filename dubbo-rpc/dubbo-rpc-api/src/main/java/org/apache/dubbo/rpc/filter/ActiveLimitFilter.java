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

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcStatus;

/**
 * LimitInvokerFilter
 */
@Activate(group = Constants.CONSUMER, value = Constants.ACTIVES_KEY)
public class ActiveLimitFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        URL url = invoker.getUrl();
        String methodName = invocation.getMethodName();
        int max = invoker.getUrl().getMethodParameter(methodName, Constants.ACTIVES_KEY, 0);
        RpcStatus count = RpcStatus.getStatus(invoker.getUrl(), invocation.getMethodName());
        if (max > 0) {
            long timeout = invoker.getUrl().getMethodParameter(invocation.getMethodName(), Constants.TIMEOUT_KEY, 0);
            long start = System.currentTimeMillis();
            long remain = timeout;
            int active = count.getActive();
            if (active >= max) {
                synchronized (count) {
                    while ((active = count.getActive()) >= max) {
                        try {
                            count.wait(remain);
                        } catch (InterruptedException e) {
                        }
                        long elapsed = System.currentTimeMillis() - start;
                        remain = timeout - elapsed;
                        if (remain <= 0) {
                            throw new RpcException("Waiting concurrent invoke timeout in client-side for service:  "
                                    + invoker.getInterface().getName() + ", method: "
                                    + invocation.getMethodName() + ", elapsed: " + elapsed
                                    + ", timeout: " + timeout + ". concurrent invokes: " + active
                                    + ". max concurrent invoke limit: " + max);
                        }
                    }
                }
            }
        }
        try {
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
        } finally {
            if (max > 0) {
                synchronized (count) {
                    count.notify();
                }
            }
        }
    }

}
