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
package org.apache.dubbo.rpc.cluster.router;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.BaseFilter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.filter.ClusterFilter;
import org.apache.dubbo.rpc.model.FrameworkModel;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER;

@Activate(group = {CONSUMER})
public class RouterSnapshotFilter implements ClusterFilter, BaseFilter.Listener {

    private final RouterSnapshotSwitcher switcher;
    private final static Logger logger = LoggerFactory.getLogger(RouterSnapshotFilter.class);

    public RouterSnapshotFilter(FrameworkModel frameworkModel) {
        this.switcher = frameworkModel.getBeanFactory().getBean(RouterSnapshotSwitcher.class);
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (!switcher.isEnable()) {
            return invoker.invoke(invocation);
        }

        if (!logger.isInfoEnabled()) {
            return invoker.invoke(invocation);
        }

        if (!switcher.isEnable(invocation.getServiceModel().getServiceKey())) {
            return invoker.invoke(invocation);
        }

        RpcContext.getServiceContext().setNeedPrintRouterSnapshot(true);
        return invoker.invoke(invocation);
    }

    @Override
    public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {
        RpcContext.getServiceContext().setNeedPrintRouterSnapshot(false);
    }

    @Override
    public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {
        RpcContext.getServiceContext().setNeedPrintRouterSnapshot(false);
    }
}
