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
package org.apache.dubbo.xds.resource.filter.rbac;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class AuthConfig {

    private final List<PolicyMatcher> policies;

    private final Action action;

    public static AuthConfig create(List<PolicyMatcher> policies, Action action) {
        return new AuthConfig(policies, action);
    }

    AuthConfig(List<PolicyMatcher> policies, Action action) {
        if (policies == null) {
            throw new NullPointerException("Null policies");
        }
        this.policies = Collections.unmodifiableList(new ArrayList<>(policies));
        if (action == null) {
            throw new NullPointerException("Null action");
        }
        this.action = action;
    }

    public List<PolicyMatcher> getPolicies() {
        return policies;
    }

    public Action getAction() {
        return action;
    }

    @Override
    public String toString() {
        return "AuthConfig{" + "policies=" + policies + ", " + "action=" + action + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof AuthConfig) {
            AuthConfig that = (AuthConfig) o;
            return this.policies.equals(that.getPolicies()) && this.action.equals(that.getAction());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= policies.hashCode();
        h$ *= 1000003;
        h$ ^= action.hashCode();
        return h$;
    }
}
