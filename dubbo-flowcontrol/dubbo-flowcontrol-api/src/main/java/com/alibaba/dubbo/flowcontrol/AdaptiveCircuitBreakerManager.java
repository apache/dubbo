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
package com.alibaba.dubbo.flowcontrol;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Adaptive;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;

import java.util.List;


@Adaptive
public class AdaptiveCircuitBreakerManager implements CircuitBreakerManager {


    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) {
        return getCircuitBreakerManager(invoker).invoke(invoker, invocation);
    }

    @Override
    public boolean isCircuitBreakerInterface(Invoker invoker, Invocation invocation) {
        return getCircuitBreakerManager(invoker).isCircuitBreakerInterface(invoker, invocation);
    }

    @Override
    public <T> List<Invoker<T>> filterCricuitBreakInvoker(List<Invoker<T>> invokers, Invocation invocation) {
        if (CollectionUtils.isEmpty(invokers)) {
            return invokers;
        }
        return getCircuitBreakerManager(invokers.get(0)).filterCricuitBreakInvoker(invokers, invocation);
    }

    @Override
    public boolean isSwitchLoadBalance(Invoker invoker, Invocation invocation) {
        return getCircuitBreakerManager(invoker).isSwitchLoadBalance(invoker, invocation);
    }


    private CircuitBreakerManager getCircuitBreakerManager(Invoker invoker) {
        ExtensionLoader extensionLoader = ExtensionLoader.getExtensionLoader(CircuitBreakerManager.class);
        String circuitBreak = invoker.getUrl().getParameter(Constants.CIRCUIT_BREAKER, Constants.DEFAULT_CIRCUIT_BREAKER);
        CircuitBreakerManager circuitBreakerManager;
        switch (Circuit.getByValue(circuitBreak)) {
            case DFIRE:
                circuitBreakerManager = (CircuitBreakerManager) extensionLoader.getExtension(circuitBreak);
                break;
            default:
                circuitBreakerManager = (CircuitBreakerManager) extensionLoader.getExtension(Constants.DEFAULT_CIRCUIT_BREAKER);
                break;
        }
        return circuitBreakerManager;
    }


}
