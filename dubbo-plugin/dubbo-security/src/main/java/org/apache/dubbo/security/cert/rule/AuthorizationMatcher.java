package org.apache.dubbo.security.cert.rule;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;

import java.util.List;

public class AuthorizationMatcher {
    public static boolean match(List<AuthorizationPolicy> policies, URL peer, URL local, Invocation invocation) {
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
