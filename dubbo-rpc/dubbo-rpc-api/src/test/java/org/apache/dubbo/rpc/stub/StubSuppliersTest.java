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

import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.model.ServiceDescriptor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.fail;

class StubSuppliersTest {

    private final String serviceName = "service";

    @Test
    void addDescriptor() {
        ServiceDescriptor descriptor = Mockito.mock(ServiceDescriptor.class);
        StubSuppliers.addDescriptor(serviceName, descriptor);
        Assertions.assertEquals(descriptor, StubSuppliers.getServiceDescriptor(serviceName));
    }

    @Test
    void addSupplier() {
        Invoker<?> invoker = Mockito.mock(Invoker.class);
        ServiceDescriptor descriptor = Mockito.mock(ServiceDescriptor.class);
        StubSuppliers.addSupplier(serviceName, i -> invoker);
        Assertions.assertEquals(invoker, StubSuppliers.createStub(serviceName, invoker));
    }

    @Test
    void createStub() {
        Invoker<?> invoker = Mockito.mock(Invoker.class);
        try {
            StubSuppliers.createStub(serviceName + 1, invoker);
            fail();
        } catch (IllegalStateException e) {
            // pass
        }
    }

    @Test
    void getServiceDescriptor() {
        try {
            StubSuppliers.getServiceDescriptor(serviceName + 1);
            fail();
        } catch (IllegalStateException e) {
            // pass
        }
    }
}
