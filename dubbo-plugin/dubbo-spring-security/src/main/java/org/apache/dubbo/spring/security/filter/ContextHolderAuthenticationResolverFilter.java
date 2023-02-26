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
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.spring.security.jackson.ObjectMapperCodec;
import org.apache.dubbo.spring.security.utils.SecurityNames;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.apache.dubbo.spring.security.utils.SecurityNames.SECURITY_CONTEXT_HOLDER_CLASS_NAME;

@Activate(group = CommonConstants.PROVIDER, order = -10000,onClass = SECURITY_CONTEXT_HOLDER_CLASS_NAME)
public class ContextHolderAuthenticationResolverFilter implements Filter {

    private final ObjectMapperCodec mapper;

    public ContextHolderAuthenticationResolverFilter(ApplicationModel applicationModel) {
        this.mapper = applicationModel.getBeanFactory().getBean(ObjectMapperCodec.class);
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        getSecurityContext(invocation);

        return invoker.invoke(invocation);
    }

    private void getSecurityContext(Invocation invocation) {
        String authenticationJSON = invocation.getAttachment(SecurityNames.SECURITY_AUTHENTICATION_CONTEXT_KEY);

        if (StringUtils.isBlank(authenticationJSON)) {
            return;
        }
        Authentication authentication = mapper.deserialize(authenticationJSON, Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

}
