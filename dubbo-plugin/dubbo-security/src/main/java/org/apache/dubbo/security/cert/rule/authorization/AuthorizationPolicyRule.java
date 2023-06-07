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

import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.security.cert.Endpoint;

public class AuthorizationPolicyRule {
    private AuthorizationSource from;
    private AuthorizationTarget to;
    private AuthorizationCondition when;

    public AuthorizationSource getFrom() {
        return from;
    }

    public void setFrom(AuthorizationSource from) {
        this.from = from;
    }

    public AuthorizationTarget getTo() {
        return to;
    }

    public void setTo(AuthorizationTarget to) {
        this.to = to;
    }

    public AuthorizationCondition getWhen() {
        return when;
    }

    public void setWhen(AuthorizationCondition when) {
        this.when = when;
    }

    public boolean match(Endpoint peer, Endpoint local, Invocation invocation) {
        if (from != null && !from.match(peer)) {
            return false;
        }
        if (to != null && !to.match(local)) {
            return false;
        }
        return when == null || when.match(invocation);
    }
}
