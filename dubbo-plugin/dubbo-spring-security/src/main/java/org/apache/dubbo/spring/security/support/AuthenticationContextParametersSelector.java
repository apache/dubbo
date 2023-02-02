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

package org.apache.dubbo.spring.security.support;

import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.PenetrateAttachmentSelector;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcContextAttachment;

import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.spring.security.utils.SecurityNames.SECURITY_AUTHENTICATION_CONTEXT_KEY;

public class AuthenticationContextParametersSelector implements PenetrateAttachmentSelector {

    @Override
    public Map<String, Object> select(Invocation invocation, RpcContextAttachment clientAttachment,
                                      RpcContextAttachment serverAttachment) {

        Map<String,Object> resultMap = RpcContext.getServerAttachment().getObjectAttachments();

        Map<String,Object> params = new HashMap<>();

        params.put(SECURITY_AUTHENTICATION_CONTEXT_KEY, resultMap.get(SECURITY_AUTHENTICATION_CONTEXT_KEY));

        return params;
    }

    @Override
    public Map<String, Object> selectReverse(Invocation invocation,
                                             RpcContextAttachment clientResponseContext,
                                             RpcContextAttachment serverResponseContext) {

        return RpcContext.getClientResponseContext().getObjectAttachments();
    }
}
