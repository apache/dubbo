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
package org.apache.dubbo.rpc.proxy.wrapper;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.support.DemoService;
import org.apache.dubbo.rpc.support.DemoServiceStub;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.apache.dubbo.common.constants.CommonConstants.STUB_EVENT_KEY;
import static org.apache.dubbo.rpc.Constants.STUB_KEY;

/**
 * {@link StubProxyFactoryWrapper }
 */
class StubProxyFactoryWrapperTest {

    @Test
    void test() {
        ProxyFactory proxyFactory = Mockito.mock(ProxyFactory.class);
        Protocol protocol = Mockito.mock(Protocol.class);
        StubProxyFactoryWrapper stubProxyFactoryWrapper = new StubProxyFactoryWrapper(proxyFactory);
        stubProxyFactoryWrapper.setProtocol(protocol);

        URL url = URL.valueOf("test://127.0.0.1/test?stub=true");
        url = url.addParameter(STUB_KEY, "true");
        url = url.addParameter(STUB_EVENT_KEY, "true");
        Invoker<DemoService> invoker = Mockito.mock(Invoker.class);
        Mockito.when(invoker.getInterface()).thenReturn(DemoService.class);
        Mockito.when(invoker.getUrl()).thenReturn(url);

        DemoService proxy = stubProxyFactoryWrapper.getProxy(invoker, false);
        Assertions.assertTrue(proxy instanceof DemoServiceStub);
        Mockito.verify(protocol, Mockito.times(1)).export(Mockito.any());

    }
}
