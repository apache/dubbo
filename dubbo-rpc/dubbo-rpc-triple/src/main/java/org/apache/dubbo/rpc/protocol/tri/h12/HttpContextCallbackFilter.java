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
package org.apache.dubbo.rpc.protocol.tri.h12;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.remoting.http12.HttpResult;
import org.apache.dubbo.remoting.http12.exception.HttpResultPayloadException;
import org.apache.dubbo.rpc.BaseFilter;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.protocol.tri.TripleConstants;

@Activate(group = CommonConstants.PROVIDER, order = 10)
public class HttpContextCallbackFilter implements Filter, BaseFilter.Listener {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        return invoker.invoke(invocation);
    }

    @Override
    public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {
        Object handlerType = invocation.get(TripleConstants.HANDLER_TYPE_KEY);
        if (handlerType == null) {
            return;
        }

        Throwable exception = appResponse.getException();
        if (exception instanceof HttpResultPayloadException) {
            Object value = TripleConstants.TRIPLE_HANDLER_TYPE_GRPC.equals(handlerType)
                    ? HttpResult.of(exception)
                    : ((HttpResultPayloadException) exception).getResult();
            appResponse.setValue(value);
            appResponse.setException(null);
            return;
        }

        HttpResponse response = (HttpResponse) invocation.get(TripleConstants.HTTP_RESPONSE_KEY);
        if (response.isEmpty()) {
            return;
        }
        if (!response.isCommitted()) {
            if (response.isContentEmpty()) {
                response.setBody(appResponse.hasException() ? appResponse.getException() : appResponse.getValue());
            }
            response.commit();
        }
        HttpResult<Object> result = response.toHttpResult();
        if (result.getBody() instanceof Throwable) {
            appResponse.setException((Throwable) result.getBody());
        } else {
            appResponse.setValue(result);
            appResponse.setException(null);
        }
    }

    @Override
    public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {}
}
