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
 * ActiveLimitFilter restrict the concurrent client invocation for a service or service's method from client side.
 * To use active limit filter, configured url with <b>actives</b> and provide valid >0 integer value.
 * <pre>
 *     e.g. <dubbo:reference id="demoService" check="false" interface="org.apache.dubbo.demo.DemoService" "actives"="2"/>
 *      In the above example maximum 2 concurrent invocation is allowed.
 *      If there are more than configured (in this example 2) is trying to invoke remote method, then rest of invocation
 *      will wait for configured timeout(default is 0 second) before invocation gets kill by dubbo.
 * </pre>
 *
 * @see Filter
 */
@Activate(group = Constants.CONSUMER, value = Constants.ACTIVES_KEY)
public class ActiveLimitFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        URL url = invoker.getUrl();
        String methodName = invocation.getMethodName();
        int max = invoker.getUrl().getMethodParameter(methodName, Constants.ACTIVES_KEY, 0);
        RpcStatus count = RpcStatus.getStatus(invoker.getUrl(), invocation.getMethodName());
        if (!RpcStatus.beginCount(url, methodName, max)) {
            long timeout = invoker.getUrl().getMethodParameter(invocation.getMethodName(), Constants.TIMEOUT_KEY, 0);
            long start = System.currentTimeMillis();
            long remain = timeout;
            synchronized (count) {
                while (!RpcStatus.beginCount(url, methodName, max)) {
                    try {
                        count.wait(remain);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                    long elapsed = System.currentTimeMillis() - start;
                    remain = timeout - elapsed;
                    if (remain <= 0) {
                        throw new RpcException("Waiting concurrent invoke timeout in client-side for service:  "
                                + invoker.getInterface().getName() + ", method: "
                                + invocation.getMethodName() + ", elapsed: " + elapsed
                                + ", timeout: " + timeout + ". concurrent invokes: " + count.getActive()
                                + ". max concurrent invoke limit: " + max);
                    }
                }
            }
        }

        boolean isSuccess = true;
        long begin = System.currentTimeMillis();
        try {
            return invoker.invoke(invocation);
        } catch (RuntimeException t) {
            isSuccess = false;
            throw t;
        } finally {
            RpcStatus.endCount(url, methodName, System.currentTimeMillis() - begin, isSuccess);
            if (max > 0) {
                synchronized (count) {
                    count.notifyAll();
                }
            }
        }
    }
}
