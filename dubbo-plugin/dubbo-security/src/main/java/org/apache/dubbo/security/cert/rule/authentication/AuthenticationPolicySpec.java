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

public class AuthenticationPolicySpec {
    private AuthenticationAction action;
    private List<AuthenticationPolicyPortLevel> portLevel;

    public AuthenticationAction getAction() {
        return action;
    }

    public void setAction(AuthenticationAction action) {
        this.action = action;
    }

    public List<AuthenticationPolicyPortLevel> getPortLevel() {
        return portLevel;
    }

    public void setPortLevel(List<AuthenticationPolicyPortLevel> portLevel) {
        this.portLevel = portLevel;
    }

    public AuthenticationAction match(int port) {
        if (portLevel == null || portLevel.isEmpty()) {
            return action;
        }

        for (AuthenticationPolicyPortLevel policyPortLevel : portLevel) {
            AuthenticationAction portPolicy = policyPortLevel.match(port);
            if (portPolicy != null) {
                return portPolicy;
            }
        }

        return action;
    }
}
