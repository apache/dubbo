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
package org.apache.dubbo.rpc.protocol.rest.rest;

import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.protocol.rest.RestRPCInvocationUtil;
import org.apache.dubbo.rpc.protocol.rest.request.RequestFacade;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Assertions;

public class TestGetInvokerServiceImpl implements TestGetInvokerService {

    @Override
    public String getInvoker() {
        Object request = RpcContext.getServiceContext().getRequest();
        RequestFacade requestFacade = (RequestFacade) request;
        Invoker invokerByRequest = RestRPCInvocationUtil.getInvokerByRequest((RequestFacade) request);

        Method hello = null;
        Method hashcode = null;
        try {
            hello = TestGetInvokerServiceImpl.class.getDeclaredMethod("getInvoker");
            hashcode = TestGetInvokerServiceImpl.class.getDeclaredMethod("hashcode");

        } catch (NoSuchMethodException e) {

        }

        Invoker invokerByServiceInvokeMethod =
                RestRPCInvocationUtil.getInvokerByServiceInvokeMethod(hello, requestFacade.getServiceDeployer());

        Invoker invoker =
                RestRPCInvocationUtil.getInvokerByServiceInvokeMethod(hashcode, requestFacade.getServiceDeployer());

        Assertions.assertEquals(invokerByRequest, invokerByServiceInvokeMethod);
        Assertions.assertNull(invoker);

        return "success";
    }
}
