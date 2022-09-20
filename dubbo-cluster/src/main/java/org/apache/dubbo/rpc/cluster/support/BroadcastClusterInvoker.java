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
package org.apache.dubbo.rpc.cluster.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.LoadBalance;

import java.util.List;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CLUSTER_ERROR_RESPONSE;
import static org.apache.dubbo.rpc.Constants.ASYNC_KEY;

/**
 * BroadcastClusterInvoker
 */
public class BroadcastClusterInvoker<T> extends AbstractClusterInvoker<T> {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(BroadcastClusterInvoker.class);
    private static final String BROADCAST_FAIL_PERCENT_KEY = "broadcast.fail.percent";
    private static final int MAX_BROADCAST_FAIL_PERCENT = 100;
    private static final int MIN_BROADCAST_FAIL_PERCENT = 0;

    public BroadcastClusterInvoker(Directory<T> directory) {
        super(directory);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Result doInvoke(final Invocation invocation, List<Invoker<T>> invokers, LoadBalance loadbalance) throws RpcException {
        checkInvokers(invokers, invocation);
        RpcContext.getServiceContext().setInvokers((List) invokers);
        RpcException exception = null;
        Result result = null;
        URL url = getUrl();
        // The value range of broadcast.fail.threshold must be 0～100.
        // 100 means that an exception will be thrown last, and 0 means that as long as an exception occurs, it will be thrown.
        // see https://github.com/apache/dubbo/pull/7174
        int broadcastFailPercent = url.getParameter(BROADCAST_FAIL_PERCENT_KEY, MAX_BROADCAST_FAIL_PERCENT);

        if (broadcastFailPercent < MIN_BROADCAST_FAIL_PERCENT || broadcastFailPercent > MAX_BROADCAST_FAIL_PERCENT) {
            logger.info(String.format("The value corresponding to the broadcast.fail.percent parameter must be between 0 and 100. " +
                    "The current setting is %s, which is reset to 100.", broadcastFailPercent));
            broadcastFailPercent = MAX_BROADCAST_FAIL_PERCENT;
        }

        int failThresholdIndex = invokers.size() * broadcastFailPercent / MAX_BROADCAST_FAIL_PERCENT;
        int failIndex = 0;
        for (Invoker<T> invoker : invokers) {
            try {
                RpcInvocation subInvocation = new RpcInvocation(invocation, invoker);
                subInvocation.setAttachment(ASYNC_KEY, "true");
                result = invokeWithContext(invoker, subInvocation);
                if (null != result && result.hasException()) {
                    Throwable resultException = result.getException();
                    if (null != resultException) {
                        exception = getRpcException(result.getException());
                        logger.warn(CLUSTER_ERROR_RESPONSE,"provider return error response","",exception.getMessage(),exception);
                        failIndex++;
                        if (failIndex == failThresholdIndex) {
                            break;
                        }
                    }
                }
            } catch (Throwable e) {
                exception = getRpcException(e);
                logger.warn(CLUSTER_ERROR_RESPONSE,"provider return error response","",exception.getMessage(),exception);
                failIndex++;
                if (failIndex == failThresholdIndex) {
                    break;
                }
            }
        }

        if (exception != null) {
            if (failIndex == failThresholdIndex) {
                if (logger.isDebugEnabled()) {
                    logger.debug(
                        String.format("The number of BroadcastCluster call failures has reached the threshold %s", failThresholdIndex));

                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("The number of BroadcastCluster call failures has not reached the threshold %s, fail size is %s",
                        failThresholdIndex, failIndex));
                }
            }
            throw exception;
        }

        return result;
    }

    private RpcException getRpcException(Throwable throwable) {
        RpcException rpcException;
        if (throwable instanceof RpcException) {
            rpcException = (RpcException) throwable;
        } else {
            rpcException = new RpcException(throwable.getMessage(), throwable);
        }
        return rpcException;
    }
}
