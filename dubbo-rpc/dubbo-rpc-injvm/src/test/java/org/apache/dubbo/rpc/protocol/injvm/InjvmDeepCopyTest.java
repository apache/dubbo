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
package org.apache.dubbo.rpc.protocol.injvm;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;

class InjvmDeepCopyTest {

    @Test
    void testDeepCopy() {
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        applicationModel.getApplicationConfigManager().setApplication(new ApplicationConfig("TestInjvm"));

        ModuleModel moduleModel = applicationModel.newModule();

        AtomicReference<Data> requestReference = new AtomicReference<>();
        AtomicReference<Data> responseReference = new AtomicReference<>();
        Demo demo = new Demo(requestReference, responseReference);

        // export provider
        ProxyFactory proxyFactory = moduleModel.getExtensionLoader(ProxyFactory.class).getExtension("javassist");
        Protocol protocol = moduleModel.getExtensionLoader(Protocol.class).getAdaptiveExtension();

        URL url = URL.valueOf("injvm://localhost:0/" + DemoInterface.class.getName() + "?interface=" + DemoInterface.class.getName());
        ServiceDescriptor providerServiceDescriptor = moduleModel.getServiceRepository().registerService(DemoInterface.class);
        ProviderModel providerModel = new ProviderModel(
            url.getServiceKey(),
            demo,
            providerServiceDescriptor,
            null,
            null);

        URL providerUrl = url.setScopeModel(moduleModel).setServiceModel(providerModel);
        Invoker invoker = proxyFactory.getInvoker(demo, DemoInterface.class, providerUrl);
        Exporter<?> exporter = protocol.export(invoker);

        // refer consumer
        ServiceDescriptor consumerServiceDescriptor = moduleModel.getServiceRepository().registerService(DemoInterface.class);
        ConsumerModel consumerModel = new ConsumerModel(DemoInterface.class.getName(), null, consumerServiceDescriptor,
            ApplicationModel.defaultModel().getDefaultModule(), null, null, ClassUtils.getClassLoader(DemoInterface.class));
        URL consumerUrl = url.setScopeModel(moduleModel).setServiceModel(consumerModel);

        DemoInterface stub = proxyFactory.getProxy(protocol.refer(DemoInterface.class, consumerUrl));

        Data request = new Data();
        Data response = stub.call(request);

        Assertions.assertNotEquals(requestReference.get(), request);
        Assertions.assertNotEquals(responseReference.get(), response);

        Data response1 = stub.call(null);

        Assertions.assertNull(requestReference.get());
        Assertions.assertNull(responseReference.get());
        Assertions.assertNull(response1);

        exporter.unexport();
        applicationModel.destroy();
    }

    interface DemoInterface {
        Data call(Data obj);
    }

    private static class Demo implements DemoInterface {
        private AtomicReference<Data> requestReference;
        private AtomicReference<Data> responseReference;

        public Demo(AtomicReference<Data> requestReference, AtomicReference<Data> responseReference) {
            this.requestReference = requestReference;
            this.responseReference = responseReference;
        }

        @Override
        public Data call(Data obj) {
            requestReference.set(obj);
            Data result = null;
            if (obj != null) {
                result = new Data();
            }
            responseReference.set(result);
            return result;
        }
    }

    private static class Data implements Serializable {

    }
}
