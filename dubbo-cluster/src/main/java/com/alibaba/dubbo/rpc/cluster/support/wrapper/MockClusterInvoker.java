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
package com.alibaba.dubbo.rpc.cluster.support.wrapper;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.rpc.*;
import com.alibaba.dubbo.rpc.cluster.Directory;
import com.alibaba.dubbo.rpc.support.MockInvoker;

import java.util.List;

/**
 * Mock Cluster Invoker 实现类
 *
 * @param <T>
 */
public class MockClusterInvoker<T> implements Invoker<T> {

    private static final Logger logger = LoggerFactory.getLogger(MockClusterInvoker.class);

    private final Directory<T> directory;
    /**
     * 真正的 Invoker 对象
     */
    private final Invoker<T> invoker;

    public MockClusterInvoker(Directory<T> directory, Invoker<T> invoker) {
        this.directory = directory;
        this.invoker = invoker;
    }

    @Override
    public URL getUrl() {
        return directory.getUrl();
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
        // 获得 "mock" 配置项，有多种配置方式
        String value = directory.getUrl().getMethodParameter(invocation.getMethodName(), Constants.MOCK_KEY, Boolean.FALSE.toString()).trim();
        //【第一种】无 mock
        if (value.length() == 0 || value.equalsIgnoreCase("false")) {
            // no mock
            // 调用原 Invoker ，发起 RPC 调用
            result = this.invoker.invoke(invocation);
        //【第二种】强制服务降级 https://dubbo.gitbooks.io/dubbo-user-book/demos/service-downgrade.html
        } else if (value.startsWith("force")) {
            if (logger.isWarnEnabled()) {
                logger.info("force-mock: " + invocation.getMethodName() + " force-mock enabled , url : " + directory.getUrl());
            }
            // force:direct mock
            // 直接调用 Mock Invoker ，执行本地 Mock 逻辑
            result = doMockInvoke(invocation, null);
        // 【第三种】失败服务降级 https://dubbo.gitbooks.io/dubbo-user-book/demos/service-downgrade.html
        } else {
            // fail-mock
            try {
                // 调用原 Invoker ，发起 RPC 调用
                result = this.invoker.invoke(invocation);
            } catch (RpcException e) {
                // 业务性异常，直接抛出
                if (e.isBiz()) {
                    throw e;
                } else {
                    if (logger.isWarnEnabled()) {
                        logger.info("fail-mock: " + invocation.getMethodName() + " fail-mock enabled , url : " + directory.getUrl(), e);
                    }
                    // 失败后，调用 Mock Invoker ，执行本地 Mock 逻辑
                    result = doMockInvoke(invocation, e);
                }
            }
        }
        return result;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Result doMockInvoke(Invocation invocation, RpcException e) {
        Result result;
        // 第一步，获得 Mock Invoker 对象
        Invoker<T> mInvoker;
        // 路由匹配 Mock Invoker 集合
        List<Invoker<T>> mockInvokers = selectMockInvoker(invocation);
        // 如果不存在，创建 MockInvoker 对象
        if (mockInvokers == null || mockInvokers.isEmpty()) {
            mInvoker = (Invoker<T>) new MockInvoker(directory.getUrl());
        // 如果存在，选择第一个
        } else {
            mInvoker = mockInvokers.get(0);
        }
        // 第二步，调用，执行本地 Mock 逻辑
        try {
            result = mInvoker.invoke(invocation);
        } catch (RpcException me) {
            if (me.isBiz()) {
                result = new RpcResult(me.getCause());
            } else {
                throw new RpcException(me.getCode(), getMockExceptionMessage(e, me), me.getCause());
            }
        } catch (Throwable me) {
            throw new RpcException(getMockExceptionMessage(e, me), me.getCause());
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
     * directory.list() will return a list of normal invokers if Constants.INVOCATION_NEED_MOCK is present in invocation, otherwise, a list of mock invokers will return.
     * if directory.list() returns more than one mock invoker, only one of them will be used.
     *
     * @param invocation
     * @return
     */
    /**
     * 返回MockInvoker
     * 契约：
     * directory 根据 invocation中 是否有 Constants.INVOCATION_NEED_MOCK ，来判断获取的是一个 normal invoker 还是一个 mock invoker
     * 如果 directorylist 返回多个 mock invoker ，只使用第一个 invoker .
     *
     * @param invocation
     * @return
     */
    private List<Invoker<T>> selectMockInvoker(Invocation invocation) {
        List<Invoker<T>> invokers = null;
        // TODO generic invoker？
        if (invocation instanceof RpcInvocation) {
            //Note the implicit contract (although the description is added to the interface declaration, but extensibility is a problem. The practice placed in the attachement needs to be improved)
            // 存在隐含契约(虽然在接口声明中增加描述，但扩展性会存在问题.同时放在 attachment 中的做法需要改进
            ((RpcInvocation) invocation).setAttachment(Constants.INVOCATION_NEED_MOCK, Boolean.TRUE.toString());
            // directory will return a list of normal invokers if Constants.INVOCATION_NEED_MOCK is present in invocation, otherwise, a list of mock invokers will return.
            // directory 根据 invocation 中 attachment 是否有 Constants.INVOCATION_NEED_MOCK，来判断获取的是 normal invokers or mock invokers
            try {
                invokers = directory.list(invocation);
            } catch (RpcException e) {
                if (logger.isInfoEnabled()) {
                    logger.info("Exception when try to invoke mock. Get mock invokers error for service:" + directory.getUrl().getServiceInterface() + ", method:" + invocation.getMethodName() + ", will contruct a new mock with 'new MockInvoker()'.", e);
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
