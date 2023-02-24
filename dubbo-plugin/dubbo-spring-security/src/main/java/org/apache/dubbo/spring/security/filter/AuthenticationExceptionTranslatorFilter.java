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
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import static org.apache.dubbo.rpc.RpcException.AUTHORIZATION_EXCEPTION;
import static org.apache.dubbo.spring.security.utils.SecurityNames.SECURITY_CONTEXT_HOLDER_CLASS_NAME;

@Activate(group = CommonConstants.PROVIDER, order =Integer.MAX_VALUE,onClass = SECURITY_CONTEXT_HOLDER_CLASS_NAME)
public class AuthenticationExceptionTranslatorFilter implements Filter, Filter.Listener {


    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        return invoker.invoke(invocation);
    }

    @Override
    public void onResponse(Result result, Invoker<?> invoker, Invocation invocation) {
        if (this.isTranslate(result)) {
            RpcException rpcException = new RpcException(result.getException().getMessage());

            rpcException.setCode(AUTHORIZATION_EXCEPTION);

            result.setException(rpcException);
        }
    }

    @Override
    public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {
    }

    private boolean isTranslate(Result result) {

        Throwable exception = result.getException();

        return result.hasException()
            && (exception instanceof AuthenticationException || exception instanceof AccessDeniedException);

    }

}
