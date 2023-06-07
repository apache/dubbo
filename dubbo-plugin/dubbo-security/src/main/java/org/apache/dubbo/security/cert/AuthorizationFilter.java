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

package org.apache.dubbo.security.cert;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.security.cert.rule.authorization.AuthorizationMatcher;
import org.apache.dubbo.security.cert.rule.authorization.AuthorizationPolicy;

import java.util.List;

@Activate(group = "provider", order = Integer.MIN_VALUE + 10000)
public class AuthorizationFilter implements Filter {
    private final FrameworkModel frameworkModel;

    private volatile AuthorityRuleSync authorityRuleSync;

    public AuthorizationFilter(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        obtainAuthorityRuleSync();
        if (authorityRuleSync == null) {
            return invoker.invoke(invocation);
        }

        Endpoints endpoints = getEndpoints(invocation);
        if (endpoints == null) {
            return invoker.invoke(invocation);
        }

        List<AuthorizationPolicy> rules = authorityRuleSync.getLatestAuthorizationPolicies();
        if (rules == null || rules.isEmpty()) {
            return invoker.invoke(invocation);
        }

        if (AuthorizationMatcher.match(rules, endpoints.getPeerEndpoint(), endpoints.getLocalEndpoint(), invocation)) {
            return invoker.invoke(invocation);
        } else {
            return AsyncRpcResult.newDefaultAsyncResult(new RpcException("Unauthorized"), invocation);
        }
    }


    private static Endpoints getEndpoints(Invocation invocation) {
        Object endpointObj = invocation.getAttributes().get("endpoints");
        if (endpointObj instanceof Endpoints) {
            return (Endpoints) endpointObj;
        }
        return null;
    }

    private void obtainAuthorityRuleSync() {
        if (authorityRuleSync == null) {
            authorityRuleSync = frameworkModel.getBeanFactory().getBean(AuthorityRuleSync.class);
        }
    }
}
