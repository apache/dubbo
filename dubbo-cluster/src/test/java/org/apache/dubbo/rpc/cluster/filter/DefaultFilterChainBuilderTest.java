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
package org.apache.dubbo.rpc.cluster.filter;


import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.protocol.AbstractInvoker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REFERENCE_FILTER_KEY;

class DefaultFilterChainBuilderTest {

    @Test
    void testBuildInvokerChainForLocalReference() {
        DefaultFilterChainBuilder defaultFilterChainBuilder = new DefaultFilterChainBuilder();

        // verify that no filter is built by default
        URL urlWithoutFilter = URL.valueOf("injvm://127.0.0.1/DemoService")
            .addParameter(INTERFACE_KEY, DemoService.class.getName());
        urlWithoutFilter = urlWithoutFilter.setScopeModel(ApplicationModel.defaultModel());
        AbstractInvoker<DemoService> invokerWithoutFilter = new AbstractInvoker<DemoService>(DemoService.class, urlWithoutFilter) {
            @Override
            protected Result doInvoke(Invocation invocation) {
                return null;
            }
        };

        Invoker<?> invokerAfterBuild = defaultFilterChainBuilder.buildInvokerChain(invokerWithoutFilter, REFERENCE_FILTER_KEY, CONSUMER);

        // verify that if LogFilter is configured, LogFilter should exist in the filter chain
        URL urlWithFilter = URL.valueOf("injvm://127.0.0.1/DemoService")
            .addParameter(INTERFACE_KEY, DemoService.class.getName())
            .addParameter(REFERENCE_FILTER_KEY, "log");
        urlWithFilter = urlWithFilter.setScopeModel(ApplicationModel.defaultModel());
        AbstractInvoker<DemoService> invokerWithFilter = new AbstractInvoker<DemoService>(DemoService.class, urlWithFilter) {
            @Override
            protected Result doInvoke(Invocation invocation) {
                return null;
            }
        };
        invokerAfterBuild = defaultFilterChainBuilder.buildInvokerChain(invokerWithFilter, REFERENCE_FILTER_KEY, CONSUMER);
        Assertions.assertTrue(invokerAfterBuild instanceof FilterChainBuilder.CallbackRegistrationInvoker);
    }

    @Test
    void testBuildInvokerChainForRemoteReference() {
        DefaultFilterChainBuilder defaultFilterChainBuilder = new DefaultFilterChainBuilder();

        // verify that no filter is built by default
        URL urlWithoutFilter = URL.valueOf("dubbo://127.0.0.1:20880/DemoService")
            .addParameter(INTERFACE_KEY, DemoService.class.getName());
        urlWithoutFilter = urlWithoutFilter.setScopeModel(ApplicationModel.defaultModel());
        AbstractInvoker<DemoService> invokerWithoutFilter = new AbstractInvoker<DemoService>(DemoService.class, urlWithoutFilter) {
            @Override
            protected Result doInvoke(Invocation invocation) {
                return null;
            }
        };

        Invoker<?> invokerAfterBuild = defaultFilterChainBuilder.buildInvokerChain(invokerWithoutFilter, REFERENCE_FILTER_KEY, CONSUMER);
//        Assertions.assertTrue(invokerAfterBuild instanceof AbstractInvoker);

        // verify that if LogFilter is configured, LogFilter should exist in the filter chain
        URL urlWithFilter = URL.valueOf("dubbo://127.0.0.1:20880/DemoService")
            .addParameter(INTERFACE_KEY, DemoService.class.getName())
            .addParameter(REFERENCE_FILTER_KEY, "log");
        urlWithFilter = urlWithFilter.setScopeModel(ApplicationModel.defaultModel());
        AbstractInvoker<DemoService> invokerWithFilter = new AbstractInvoker<DemoService>(DemoService.class, urlWithFilter) {
            @Override
            protected Result doInvoke(Invocation invocation) {
                return null;
            }
        };
        invokerAfterBuild = defaultFilterChainBuilder.buildInvokerChain(invokerWithFilter, REFERENCE_FILTER_KEY, CONSUMER);
        Assertions.assertTrue(invokerAfterBuild instanceof FilterChainBuilder.CallbackRegistrationInvoker);

    }
}
