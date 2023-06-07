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

package org.apache.dubbo.security.cert.rule.authentication;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AuthenticationPoliciesWithRevision {
    private final List<AuthenticationPolicy> authenticationPolicies;
    private final long revision;

    private AuthenticationPoliciesWithRevision(List<AuthenticationPolicy> authenticationPolicies, long revision) {
        this.authenticationPolicies = authenticationPolicies;
        this.revision = revision;
    }

    public static AuthenticationPoliciesWithRevision of(String rawRule, long revision) {
        List<AuthenticationPolicy> authenticationPolicies = AuthenticationPolicy.parse(rawRule)
            .stream()
            .filter(p -> Objects.nonNull(p.getSpec()))
            .collect(Collectors.toList());
        return new AuthenticationPoliciesWithRevision(authenticationPolicies, revision);
    }

    public List<AuthenticationPolicy> getAuthenticationPolicies() {
        return authenticationPolicies;
    }

    public long getRevision() {
        return revision;
    }
}
