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

package org.apache.dubbo.rpc.protocol.dubbo;

import org.apache.dubbo.remoting.ExceptionProcessor;
import org.apache.dubbo.remoting.RetryHandleException;
import org.apache.dubbo.remoting.ServiceNotFoundException;
import org.apache.dubbo.remoting.exchange.ExchangeChannel;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.rpc.service.OmnipotentService;
import org.apache.dubbo.rpc.support.RpcUtils;

import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_VERSION;
import static org.apache.dubbo.common.constants.CommonConstants.METHOD_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.$INVOKE_ASYNC;
import static org.apache.dubbo.common.constants.CommonConstants.$INVOKE;
import static org.apache.dubbo.common.constants.CommonConstants.GENERIC_PARAMETER_TYPES;
import static org.apache.dubbo.rpc.Constants.GENERIC_KEY;

/**
 * <p>The default exception handler interrupts the return of the client error message by throwing a {@link RetryHandleException},
 * so that there is a chance to enter the decode process again.
 * <p>Then handle the {@link ServiceNotFoundException} through server-side generalization {@link OmnipotentService}
 * (different from the original generalization, which can accept any interface)
 *
 * @since 3.1.4
 */
public class ServiceNotFoundExceptionProcessor implements ExceptionProcessor {

    private static final String DEFAULT_OMNIPOTENT_SERVICE = OmnipotentService.class.getName();
    private static final String ORIGIN_PATH_KEY = "originPath";
    private static final String ORIGIN_VERSION_KEY = "originVersion";
    private static final String ORIGIN_METHOD_KEY = "originMethod";


    @Override
    public boolean shouldReturnError(Throwable data) {
        return data instanceof ServiceNotFoundException;
    }

    @Override
    public String wrapAndHandleException(ExchangeChannel channel, Request req) throws RetryHandleException {
        Object data = req.getData();
        if (!(data instanceof DecodeableRpcInvocation)) {
            return null;
        }
        DecodeableRpcInvocation invocation = (DecodeableRpcInvocation) data;

        invocation.setAttachment(ORIGIN_PATH_KEY, invocation.getAttachment(PATH_KEY));
        // Replace serviceName in req with omn
        invocation.setAttachment(PATH_KEY, DEFAULT_OMNIPOTENT_SERVICE);
        invocation.setAttachment(INTERFACE_KEY, DEFAULT_OMNIPOTENT_SERVICE);

        invocation.setAttachment(ORIGIN_VERSION_KEY, invocation.getAttachment(VERSION_KEY));
        invocation.setAttachment(VERSION_KEY, DEFAULT_VERSION);

        invocation.setAttachment(ORIGIN_METHOD_KEY, invocation.getMethodName());
        if (RpcUtils.isReturnTypeFuture(invocation)) {
            invocation.setAttachment(METHOD_KEY, $INVOKE_ASYNC);
            invocation.setMethodName($INVOKE_ASYNC);
        } else {
            invocation.setAttachment(METHOD_KEY, $INVOKE);
            invocation.setMethodName($INVOKE);
        }

        invocation.setParameterTypes(GENERIC_PARAMETER_TYPES);

        invocation.setAttachment(
                GENERIC_KEY, Boolean.TRUE.toString());
        invocation.resetHasDecoded();

        throw new RetryHandleException(channel, "Service not found:" + invocation.getAttachment(ORIGIN_PATH_KEY) + ", " + invocation.getAttachment(ORIGIN_METHOD_KEY));
    }

}
