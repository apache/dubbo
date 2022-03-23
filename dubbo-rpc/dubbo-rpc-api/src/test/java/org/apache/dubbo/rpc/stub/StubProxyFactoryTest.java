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

package org.apache.dubbo.rpc.stub;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.ServerService;
import org.apache.dubbo.rpc.model.ServiceDescriptor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.when;

class StubProxyFactoryTest {

    private final StubProxyFactory factory = new StubProxyFactory();
    private final Invoker<MockInterface> invoker2 = Mockito.mock(Invoker.class);


    @Test
    void getProxy() {
        Invoker<?> invoker = Mockito.mock(Invoker.class);
        URL url = Mockito.mock(URL.class);
        when(invoker.getUrl())
            .thenReturn(url);
        String service = "SERV_PROX";
        when(url.getServiceInterface())
            .thenReturn(service);
        StubSuppliers.addSupplier(service, i -> invoker);
        Assertions.assertEquals(invoker, factory.getProxy(invoker));
        Assertions.assertEquals(invoker, factory.getProxy(invoker, false));
    }

    private interface MockInterface {

    }

    private class MockStub implements ServerService<MockInterface>, MockInterface {

        @Override
        public Invoker<MockInterface> getInvoker(URL url) {
            return invoker2;
        }

        @Override
        public ServiceDescriptor getServiceDescriptor() {
            return null;
        }
    }

    @Test
    void getInvoker() {
        URL url = Mockito.mock(URL.class);
        Assertions.assertEquals(invoker2,
            factory.getInvoker(new MockStub(), MockInterface.class, url));
    }
}
