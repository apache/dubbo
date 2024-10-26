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
package org.apache.dubbo.auth.filter;

import org.apache.dubbo.auth.Constants;
import org.apache.dubbo.auth.spi.Authenticator;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.HeaderFilter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.FrameworkModel;

import static org.apache.dubbo.rpc.RpcException.AUTHORIZATION_EXCEPTION;

@Activate(value = Constants.AUTH_KEY, order = -20000)
public class ProviderAuthHeaderFilter implements HeaderFilter {
    private final FrameworkModel frameworkModel;

    public ProviderAuthHeaderFilter(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    @Override
    public RpcInvocation invoke(Invoker<?> invoker, RpcInvocation invocation) throws RpcException {
        URL url = invoker.getUrl();
        boolean shouldAuth = url.getParameter(Constants.AUTH_KEY, false);
        if (shouldAuth) {
            Authenticator authenticator = frameworkModel
                    .getExtensionLoader(Authenticator.class)
                    .getExtension(url.getParameter(Constants.AUTHENTICATOR_KEY, Constants.DEFAULT_AUTHENTICATOR));
            try {
                authenticator.authenticate(invocation, url);
            } catch (Exception e) {
                throw new RpcException(AUTHORIZATION_EXCEPTION, "No Auth.");
            }
            invocation.getAttributes().put(Constants.AUTH_SUCCESS, Boolean.TRUE);
        }
        return invocation;
    }
}
