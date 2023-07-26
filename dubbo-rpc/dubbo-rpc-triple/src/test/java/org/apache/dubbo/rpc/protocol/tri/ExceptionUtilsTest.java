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

package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.model.*;
import org.apache.dubbo.rpc.protocol.tri.support.IGreeter2;
import org.apache.dubbo.rpc.protocol.tri.support.IGreeter2Impl;
import org.apache.dubbo.rpc.protocol.tri.support.IGreeterException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class ExceptionUtilsTest {

    private IllegalStateException exception = new IllegalStateException("Exception0");
    @Test
    void getStackTrace() {
        Assertions.assertTrue(ExceptionUtils.getStackTrace(exception).contains("Exception0"));
    }

    @Test
    void getStackFrameString() {
        String str = ExceptionUtils.getStackFrameString(
            Arrays.stream(exception.getStackTrace())
                .map(StackTraceElement::toString)
                .collect(Collectors.toList()));
        Assertions.assertTrue(str.contains("ExceptionUtilsTest"));
    }

    @Test
    void getStackFrames() {
        StackTraceElement[] traces = exception.getStackTrace();
        List<String> frames = Arrays.stream(traces)
            .map(StackTraceElement::toString)
            .collect(Collectors.toList());
        String str = ExceptionUtils.getStackFrameString(frames);
        List<String> stackFrames = Arrays.stream(ExceptionUtils.getStackFrames(str))
                .collect(Collectors.toList());
        Assertions.assertEquals(frames,stackFrames);
    }

    @Test
    void testGetStackFrames() {
        String[] stackFrames = ExceptionUtils.getStackFrames(exception);
        Assertions.assertNotEquals(0,stackFrames.length);
    }

    @Test
    void getStackFrameList() {
        List<String> stackFrameList = ExceptionUtils.getStackFrameList(exception, 10);
        Assertions.assertEquals(10,stackFrameList.size());
    }

    @Test
    void testGetStackFrameList() {
        List<String> stackFrameList = ExceptionUtils.getStackFrameList(exception);
        Assertions.assertNotEquals(10,stackFrameList.size());
    }

    @Test
    void testSelfDefineException() throws Exception{
        IGreeter2 serviceImpl = new IGreeter2Impl();

        int availablePort = NetUtils.getAvailablePort();
        ApplicationModel applicationModel = ApplicationModel.defaultModel();

        URL providerUrl = URL.valueOf(
            "tri://127.0.0.1:" + availablePort + "/" + IGreeter2.class.getName()).addParameter(CommonConstants.TIMEOUT_KEY, 10000);;

        ModuleServiceRepository serviceRepository = applicationModel.getDefaultModule()
            .getServiceRepository();
        ServiceDescriptor serviceDescriptor = serviceRepository.registerService(IGreeter2.class);

        ProviderModel providerModel = new ProviderModel(
            providerUrl.getServiceKey(),
            serviceImpl,
            serviceDescriptor,
            new ServiceMetadata(), ClassUtils.getClassLoader(IGreeter2.class));
        serviceRepository.registerProvider(providerModel);
        providerUrl = providerUrl.setServiceModel(providerModel);

        Protocol protocol = new TripleProtocol(providerUrl.getOrDefaultFrameworkModel());
        ProxyFactory proxy = applicationModel.getExtensionLoader(ProxyFactory.class)
            .getAdaptiveExtension();
        Invoker<IGreeter2> invoker = proxy.getInvoker(serviceImpl, IGreeter2.class, providerUrl);
        Exporter<IGreeter2> export = protocol.export(invoker);

        URL consumerUrl = URL.valueOf(
            "tri://127.0.0.1:" + availablePort + "/" + IGreeter2.class.getName()).addParameter(CommonConstants.TIMEOUT_KEY, 10000);

        ConsumerModel consumerModel = new ConsumerModel(consumerUrl.getServiceKey(), null,
            serviceDescriptor, null,
            null, null);
        consumerUrl = consumerUrl.setServiceModel(consumerModel);
        IGreeter2 greeterProxy = proxy.getProxy(protocol.refer(IGreeter2.class, consumerUrl));
        Thread.sleep(1000);

        // 1. test unaryStream
        String REQUEST_MSG = "hello world";
        String EXPECT_RESPONSE_MSG = "I am self define exception";
        try {
            greeterProxy.echo(REQUEST_MSG);
        } catch (IGreeterException e) {
            Assertions.assertEquals(EXPECT_RESPONSE_MSG, e.getMessage());
        }

        Exception e = greeterProxy.echoException(REQUEST_MSG);
        Assertions.assertEquals(EXPECT_RESPONSE_MSG, e.getMessage());

        export.unexport();
        protocol.destroy();
        // resource recycle.
        serviceRepository.destroy();
        System.out.println("serviceRepository destroyed");

    }
}
