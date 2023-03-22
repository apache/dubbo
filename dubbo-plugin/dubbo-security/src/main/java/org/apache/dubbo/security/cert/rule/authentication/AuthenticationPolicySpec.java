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
