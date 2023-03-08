package org.apache.dubbo.security.cert.rule;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;

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

    public boolean match(URL peer, URL local, Invocation invocation) {
        if (from != null && !from.match(local)) {
            return false;
        }
        if (to != null && !to.match(peer)) {
            return false;
        }
        return when == null || when.match(invocation);
    }
}
