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

package org.apache.dubbo.security.cert.rule.authorization;

import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.security.cert.Endpoint;

import java.util.List;

public class AuthorizationPolicy {
    private String name;
    private AuthorizationPolicySpec spec;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AuthorizationPolicySpec getSpec() {
        return spec;
    }

    public void setSpec(AuthorizationPolicySpec spec) {
        this.spec = spec;
    }

    public AuthorizationAction match(Endpoint peer, Endpoint local, Invocation invocation) {
        if (spec == null) {
            return AuthorizationAction.ALLOW;
        }
        return spec.match(peer, local, invocation);
    }

    public static List<AuthorizationPolicy> parse(String raw) {
        return JsonUtils.toJavaList(raw, AuthorizationPolicy.class);
    }
}
