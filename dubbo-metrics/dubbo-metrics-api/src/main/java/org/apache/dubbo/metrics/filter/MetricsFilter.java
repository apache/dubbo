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

package org.apache.dubbo.metrics.filter;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelAware;

import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER;

@Activate(group = PROVIDER, order = -1)
public class MetricsFilter implements Filter, ScopeModelAware {

    private DefaultMetricsCollector collector = null;

    private ApplicationModel applicationModel;

    @Override
    public void setApplicationModel(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;

        collector = applicationModel.getBeanFactory().getBean(DefaultMetricsCollector.class);
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String serviceUniqueName = invocation.getTargetServiceUniqueName();
        String methodName = invocation.getMethodName();
        String parameterTypesDesc = ReflectUtils.getDesc(invocation.getParameterTypes());
        String group = null;
        String interfaceAndVersion;
        String[] arr = serviceUniqueName.split("/");
        if (arr.length == 2) {
            group = arr[0];
            interfaceAndVersion = arr[1];
        } else {
            interfaceAndVersion = arr[0];
        }

        String interfaceName = interfaceAndVersion.split(":")[0];
        String version = interfaceAndVersion.split(":")[1];
        collector.increaseTotalRequests(interfaceName, methodName, parameterTypesDesc, group, version);
        collector.increaseProcessingRequests(interfaceName, methodName, parameterTypesDesc, group, version);

        Long startTime = System.currentTimeMillis();
        try {
            Result invoke = invoker.invoke(invocation);
            collector.increaseSucceedRequests(interfaceName, methodName, parameterTypesDesc, group, version);
            return invoke;
        } catch (RpcException e) {
            collector.increaseFailedRequests(interfaceName, methodName, parameterTypesDesc, group, version);
            throw e;
        } finally {
            Long endTime = System.currentTimeMillis();
            Long rt = endTime - startTime;
            collector.setRT(interfaceName, methodName, parameterTypesDesc, group, version, rt);
            collector.decreaseProcessingRequests(interfaceName, methodName, parameterTypesDesc, group, version);
        }
    }
}
