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
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.cluster.filter.ClusterFilter;
import org.apache.dubbo.spring.security.utils.SecurityNames;

import java.util.Map;
import java.util.Objects;

import static org.apache.dubbo.spring.security.utils.SecurityNames.SECURITY_AUTHENTICATION_CONTEXT_KEY;

@Activate(group = CommonConstants.CONSUMER, order = -1)
public class ContextHolderParametersSelectedTransferFilter implements ClusterFilter{

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) {
        this.setSecurityContextIfExists(invocation);

        return invoker.invoke(invocation);
    }

    private void setSecurityContextIfExists(Invocation invocation) {
        Map<String,Object> resultMap = RpcContext.getServerAttachment().getObjectAttachments();

        Object authentication = resultMap.get(SECURITY_AUTHENTICATION_CONTEXT_KEY);

        if (Objects.isNull(authentication)) {
            return ;
        }

        invocation.setObjectAttachment(SecurityNames.SECURITY_AUTHENTICATION_CONTEXT_KEY, authentication);
    }
}
