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

package org.apache.dubbo.spring.security.filter;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

import static org.apache.dubbo.rpc.RpcException.FORBIDDEN_EXCEPTION;

@Activate(group = CommonConstants.PROVIDER, order =Integer.MAX_VALUE)
public class SecurityExceptionTranslationFilter implements Filter, BaseFilter.Listener {


    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        return invoker.invoke(invocation);
    }

    @Override
    public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {
        if (appResponse.hasException() && this.isMatchException(appResponse.getException())) {
            this.handleSecurityException(appResponse.getException(), appResponse);
        }
    }

    @Override
    public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {
    }

    private void handleSecurityException(Throwable exception, Result result) {
        RpcException rpcException = new RpcException(exception.getMessage());

        rpcException.setCode(FORBIDDEN_EXCEPTION);

        result.setException(rpcException);
    }

    private boolean isMatchException(Throwable exception) {
        return exception instanceof AuthenticationException
            || exception instanceof AccessDeniedException;

    }

}
