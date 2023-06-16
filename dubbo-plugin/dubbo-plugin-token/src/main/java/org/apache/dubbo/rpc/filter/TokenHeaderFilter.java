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
package org.apache.dubbo.rpc.filter;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.rpc.HeaderFilter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;

import static org.apache.dubbo.rpc.Constants.TOKEN_KEY;
import static org.apache.dubbo.rpc.RpcException.FORBIDDEN_EXCEPTION;

@Activate
public class TokenHeaderFilter implements HeaderFilter {
    @Override
    public RpcInvocation invoke(Invoker<?> invoker, RpcInvocation invocation) throws RpcException {
        String token = invoker.getUrl().getParameter(TOKEN_KEY);
        if (ConfigUtils.isNotEmpty(token)) {
            Class<?> serviceType = invoker.getInterface();
            String remoteToken = (String) invocation.getObjectAttachmentWithoutConvert(TOKEN_KEY);
            if (!token.equals(remoteToken)) {
                throw new RpcException(FORBIDDEN_EXCEPTION, "Forbid invoke remote service " + serviceType + " method " + invocation.getMethodName() +
                    "() from consumer " + RpcContext.getServiceContext().getRemoteHost() + " to provider " +
                    RpcContext.getServiceContext().getLocalHost() + ", consumer incorrect token is " + remoteToken);
            }
        }
        return invocation;
    }
}
