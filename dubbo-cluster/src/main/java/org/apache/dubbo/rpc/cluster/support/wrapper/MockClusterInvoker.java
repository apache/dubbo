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
package org.apache.dubbo.rpc.cluster.support.wrapper;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.InvokeMode;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.ClusterInvoker;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.protocol.dubbo.FutureAdapter;
import org.apache.dubbo.rpc.support.MockInvoker;
import org.apache.dubbo.rpc.support.RpcUtils;

import java.util.List;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CLUSTER_FAILED_MOCK_REQUEST;
import static org.apache.dubbo.rpc.Constants.MOCK_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.FORCE_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.INVOCATION_NEED_MOCK;

public class MockClusterInvoker<T> implements ClusterInvoker<T> {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(MockClusterInvoker.class);
    private static final boolean setFutureWhenSync = Boolean.parseBoolean(System.getProperty(CommonConstants.SET_FUTURE_IN_SYNC_MODE, "true"));

    private final Directory<T> directory;

    private final Invoker<T> invoker;

    public MockClusterInvoker(Directory<T> directory, Invoker<T> invoker) {
        this.directory = directory;
        this.invoker = invoker;
    }

    @Override
    public URL getUrl() {
        return directory.getConsumerUrl();
    }

    @Override
    public URL getRegistryUrl() {
        return directory.getUrl();
    }

    @Override
    public Directory<T> getDirectory() {
        return directory;
    }

    @Override
    public boolean isDestroyed() {
        return directory.isDestroyed();
    }

    @Override
    public boolean isAvailable() {
        return directory.isAvailable();
    }

    @Override
    public void destroy() {
        this.invoker.destroy();
    }

    @Override
    public Class<T> getInterface() {
        return directory.getInterface();
    }

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        Result result;

        String value = getUrl().getMethodParameter(invocation.getMethodName(), MOCK_KEY, Boolean.FALSE.toString()).trim();
        if (ConfigUtils.isEmpty(value)) {
            //no mock
            result = this.invoker.invoke(invocation);
        } else if (value.startsWith(FORCE_KEY)) {
            if (logger.isWarnEnabled()) {
                logger.warn(CLUSTER_FAILED_MOCK_REQUEST,"force mock","","force-mock: " + invocation.getMethodName() + " force-mock enabled , url : " + getUrl());
            }
            //force:direct mock
            result = doMockInvoke(invocation, null);
        } else {
            //fail-mock
            try {
                result = this.invoker.invoke(invocation);

                //fix:#4585
                if (result.getException() != null && result.getException() instanceof RpcException) {
                    RpcException rpcException = (RpcException) result.getException();
                    if (rpcException.isBiz()) {
                        throw rpcException;
                    } else {
                        result = doMockInvoke(invocation, rpcException);
                    }
                }

            } catch (RpcException e) {
                if (e.isBiz()) {
                    throw e;
                }

                if (logger.isWarnEnabled()) {
                    logger.warn(CLUSTER_FAILED_MOCK_REQUEST,"failed to mock invoke","","fail-mock: " + invocation.getMethodName() + " fail-mock enabled , url : " + getUrl(),e);
                }
                result = doMockInvoke(invocation, e);
            }
        }
        return result;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Result doMockInvoke(Invocation invocation, RpcException e) {
        Result result;
        Invoker<T> mockInvoker;

        RpcInvocation rpcInvocation = (RpcInvocation)invocation;
        rpcInvocation.setInvokeMode(RpcUtils.getInvokeMode(getUrl(),invocation));

        List<Invoker<T>> mockInvokers = selectMockInvoker(invocation);
        if (CollectionUtils.isEmpty(mockInvokers)) {
            mockInvoker = (Invoker<T>) new MockInvoker(getUrl(), directory.getInterface());
        } else {
            mockInvoker = mockInvokers.get(0);
        }
        try {
            result = mockInvoker.invoke(invocation);
        } catch (RpcException mockException) {
            if (mockException.isBiz()) {
                result = AsyncRpcResult.newDefaultAsyncResult(mockException.getCause(), invocation);
            } else {
                throw new RpcException(mockException.getCode(), getMockExceptionMessage(e, mockException), mockException.getCause());
            }
        } catch (Throwable me) {
            throw new RpcException(getMockExceptionMessage(e, me), me.getCause());
        }
        if (setFutureWhenSync || rpcInvocation.getInvokeMode() != InvokeMode.SYNC) {
            // set server context
            RpcContext.getServiceContext().setFuture(new FutureAdapter<>(((AsyncRpcResult)result).getResponseFuture()));
        }
        return result;
    }

    private String getMockExceptionMessage(Throwable t, Throwable mt) {
        String msg = "mock error : " + mt.getMessage();
        if (t != null) {
            msg = msg + ", invoke error is :" + StringUtils.toString(t);
        }
        return msg;
    }

    /**
     * Return MockInvoker
     * Contract：
     * directory.list() will return a list of normal invokers if Constants.INVOCATION_NEED_MOCK is absent or not true in invocation, otherwise, a list of mock invokers will return.
     * if directory.list() returns more than one mock invoker, only one of them will be used.
     *
     * @param invocation
     * @return
     */
    private List<Invoker<T>> selectMockInvoker(Invocation invocation) {
        List<Invoker<T>> invokers = null;
        //TODO generic invoker？
        if (invocation instanceof RpcInvocation) {
            //Note the implicit contract (although the description is added to the interface declaration, but extensibility is a problem. The practice placed in the attachment needs to be improved)
            invocation.setAttachment(INVOCATION_NEED_MOCK, Boolean.TRUE.toString());
            //directory will return a list of normal invokers if Constants.INVOCATION_NEED_MOCK is absent or not true in invocation, otherwise, a list of mock invokers will return.
            try {
                RpcContext.getServiceContext().setConsumerUrl(getUrl());
                invokers = directory.list(invocation);
            } catch (RpcException e) {
                if (logger.isInfoEnabled()) {
                    logger.info("Exception when try to invoke mock. Get mock invokers error for service:"
                            + getUrl().getServiceInterface() + ", method:" + invocation.getMethodName()
                            + ", will construct a new mock with 'new MockInvoker()'.", e);
                }
            }
        }
        return invokers;
    }

    @Override
    public String toString() {
        return "invoker :" + this.invoker + ",directory: " + this.directory;
    }
}
