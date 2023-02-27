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
package org.apache.dubbo.rpc.protocol;


import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.listener.ListenerInvokerWrapper;
import org.apache.dubbo.rpc.proxy.DemoService;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INVOKER_LISTENER_KEY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProtocolListenerWrapperTest {

    @Test
    void testLoadingListenerForLocalReference() {
        // verify that no listener is loaded by default
        URL urlWithoutListener = URL.valueOf("injvm://127.0.0.1/DemoService")
            .addParameter(INTERFACE_KEY, DemoService.class.getName());
        AbstractInvoker<DemoService> invokerWithoutListener = new AbstractInvoker<DemoService>(DemoService.class, urlWithoutListener) {
            @Override
            protected Result doInvoke(Invocation invocation) throws Throwable {
                return null;
            }
        };

        Protocol protocolWithoutListener = mock(Protocol.class);
        when(protocolWithoutListener.refer(DemoService.class, urlWithoutListener)).thenReturn(invokerWithoutListener);

        ProtocolListenerWrapper protocolListenerWrapperWithoutListener = new ProtocolListenerWrapper(protocolWithoutListener);

        Invoker<?> invoker = protocolListenerWrapperWithoutListener.refer(DemoService.class, urlWithoutListener);
        Assertions.assertTrue(invoker instanceof ListenerInvokerWrapper);
        Assertions.assertEquals(0, ((ListenerInvokerWrapper<?>) invoker).getListeners().size());

        // verify that if the invoker.listener is configured, then load the specified listener
        URL urlWithListener = URL.valueOf("injvm://127.0.0.1/DemoService")
            .addParameter(INTERFACE_KEY, DemoService.class.getName())
            .addParameter(INVOKER_LISTENER_KEY, "count");
        AbstractInvoker<DemoService> invokerWithListener = new AbstractInvoker<DemoService>(DemoService.class, urlWithListener) {
            @Override
            protected Result doInvoke(Invocation invocation) throws Throwable {
                return null;
            }
        };

        Protocol protocol = mock(Protocol.class);
        when(protocol.refer(DemoService.class, urlWithListener)).thenReturn(invokerWithListener);

        ProtocolListenerWrapper protocolListenerWrapper = new ProtocolListenerWrapper(protocol);

        invoker = protocolListenerWrapper.refer(DemoService.class, urlWithListener);
        Assertions.assertTrue(invoker instanceof ListenerInvokerWrapper);
        Assertions.assertEquals(1, CountInvokerListener.getCounter());
    }

    @Test
    void testLoadingListenerForRemoteReference() {
        // verify that no listener is loaded by default
        URL urlWithoutListener = URL.valueOf("dubbo://127.0.0.1:20880/DemoService")
            .addParameter(INTERFACE_KEY, DemoService.class.getName());
        AbstractInvoker<DemoService> invokerWithoutListener = new AbstractInvoker<DemoService>(DemoService.class, urlWithoutListener) {
            @Override
            protected Result doInvoke(Invocation invocation) throws Throwable {
                return null;
            }
        };

        Protocol protocolWithoutListener = mock(Protocol.class);
        when(protocolWithoutListener.refer(DemoService.class, urlWithoutListener)).thenReturn(invokerWithoutListener);

        ProtocolListenerWrapper protocolListenerWrapperWithoutListener = new ProtocolListenerWrapper(protocolWithoutListener);

        Invoker<?> invoker = protocolListenerWrapperWithoutListener.refer(DemoService.class, urlWithoutListener);
        Assertions.assertTrue(invoker instanceof ListenerInvokerWrapper);
        Assertions.assertEquals(0, ((ListenerInvokerWrapper<?>) invoker).getListeners().size());

        // verify that if the invoker.listener is configured, then load the specified listener
        URL urlWithListener = URL.valueOf("dubbo://127.0.0.1:20880/DemoService")
            .addParameter(INTERFACE_KEY, DemoService.class.getName())
            .addParameter(INVOKER_LISTENER_KEY, "count");
        AbstractInvoker<DemoService> invokerWithListener = new AbstractInvoker<DemoService>(DemoService.class, urlWithListener) {
            @Override
            protected Result doInvoke(Invocation invocation) throws Throwable {
                return null;
            }
        };

        Protocol protocol = mock(Protocol.class);
        when(protocol.refer(DemoService.class, urlWithListener)).thenReturn(invokerWithListener);

        ProtocolListenerWrapper protocolListenerWrapper = new ProtocolListenerWrapper(protocol);

        invoker = protocolListenerWrapper.refer(DemoService.class, urlWithListener);
        Assertions.assertTrue(invoker instanceof ListenerInvokerWrapper);
        Assertions.assertEquals(1, CountInvokerListener.getCounter());
    }


}
