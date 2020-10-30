/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.config.invoker;

import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.api.Greeting;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;

public class DelegateProviderMetaDataInvokerTest {
    private ServiceConfig service;
    private Invoker<Greeting> invoker;

    @BeforeEach
    public void setUp() throws Exception {
        service = Mockito.mock(ServiceConfig.class);
        invoker = Mockito.mock(Invoker.class);
    }

    @Test
    public void testDelegate() throws Exception {
        DelegateProviderMetaDataInvoker<Greeting> delegate =
                new DelegateProviderMetaDataInvoker<Greeting>(invoker, service);
        delegate.getInterface();
        Mockito.verify(invoker).getInterface();
        delegate.getUrl();
        Mockito.verify(invoker).getUrl();
        delegate.isAvailable();
        Mockito.verify(invoker).isAvailable();
        Invocation invocation = Mockito.mock(Invocation.class);
        delegate.invoke(invocation);
        Mockito.verify(invoker).invoke(invocation);
        delegate.destroy();
        Mockito.verify(invoker).destroy();
        assertThat(delegate.getMetadata(), sameInstance(service));
    }

}
