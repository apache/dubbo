package org.apache.dubbo.security.cert.rule.authorization;

import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.security.cert.Endpoint;

import java.util.List;

public class AuthorizationMatcher {
    public static boolean match(List<AuthorizationPolicy> policies, Endpoint peer, Endpoint local, Invocation invocation) {
        if (policies.isEmpty()) {
            return true;
        }

        for (AuthorizationPolicy policy : policies) {
            AuthorizationAction action = policy.match(peer, local, invocation);
            if (action == AuthorizationAction.DENY) {
                return false;
            }
        }

        return true;
    }
}
