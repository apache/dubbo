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
package org.apache.dubbo.xds.resource_new.filter.rbac;

import org.apache.dubbo.common.lang.Nullable;

final class AuthDecision {

    private final Action decision;

    @Nullable
    private final String matchingPolicyName;

    static AuthDecision create(Action decisionType, @Nullable String matchingPolicy) {
        return new AuthDecision(decisionType, matchingPolicy);
    }

    AuthDecision(Action decision, @Nullable String matchingPolicyName) {
        if (decision == null) {
            throw new NullPointerException("Null decision");
        }
        this.decision = decision;
        this.matchingPolicyName = matchingPolicyName;
    }

    public Action decision() {
        return decision;
    }

    @Nullable
    public String matchingPolicyName() {
        return matchingPolicyName;
    }

    @Override
    public String toString() {
        return "AuthDecision{" + "decision=" + decision + ", " + "matchingPolicyName=" + matchingPolicyName + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof AuthDecision) {
            AuthDecision that = (AuthDecision) o;
            return this.decision.equals(that.decision())
                    && (this.matchingPolicyName == null
                            ? that.matchingPolicyName() == null
                            : this.matchingPolicyName.equals(that.matchingPolicyName()));
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= decision.hashCode();
        h$ *= 1000003;
        h$ ^= (matchingPolicyName == null) ? 0 : matchingPolicyName.hashCode();
        return h$;
    }
}
