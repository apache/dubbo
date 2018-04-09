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

import com.alibaba.dubbo.common.extension.SPI;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;

import java.util.List;

@SPI
public interface CircuitBreakerManager {

    /**
     *
     * @param invoker
     * @param invocation
     * @return
     */
    Result invoke(Invoker<?> invoker, Invocation invocation);

    /**
     * 是否为熔断接口 --- concurrentHashMap中存在且已经发生了熔断包括 open 和 halfopen
     * @param invoker
     * @param invocation
     * @return
     */
     boolean isCircuitBreakerInterface(Invoker invoker, Invocation invocation);

    /**
     *过滤 服务接口
     * @param invokers
     * @param invocation
     * @param <T>
     * @return
     */
     <T> List<Invoker<T>> filterCricuitBreakInvoker(List<Invoker<T>> invokers, Invocation invocation);

    /**
     * 如果存在 HalfOpen 可重试接口
     * @param invoker
     * @param invocation
     * @return
     */
      boolean isSwitchLoadBalance(Invoker invoker, Invocation invocation);

}
