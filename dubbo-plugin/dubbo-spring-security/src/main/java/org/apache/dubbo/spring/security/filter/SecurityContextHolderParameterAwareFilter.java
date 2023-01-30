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
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.spring.security.utils.SecurityKeyNames;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;

@Activate(group = CommonConstants.PROVIDER, order = -10000)
public class SecurityContextHolderParameterAwareFilter implements Filter {
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        getSecurityContext(invocation);

        return invoker.invoke(invocation);
    }

    private static void getSecurityContext(Invocation invocation) {
        Object authentication = invocation.getObjectAttachment(SecurityKeyNames.SECURITY_AUTHENTICATION_CONTEXT_KEY);

        if (Objects.nonNull(authentication)
            && ClassUtils.isAssignableFrom(Authentication.class, authentication.getClass())) {

            SecurityContextHolder.getContext().setAuthentication((Authentication) authentication);
        }
    }

}
