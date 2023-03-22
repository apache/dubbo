package org.apache.dubbo.security.cert.rule.authorization;

import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.security.cert.Endpoint;

import java.util.List;

public class AuthorizationPolicySpec {
    private AuthorizationAction action;
    private List<AuthorizationPolicyRule> rules;
    private double samples;
    private AuthorizationMatchType matchType;

    public AuthorizationAction getAction() {
        return action;
    }

    public void setAction(AuthorizationAction action) {
        this.action = action;
    }

    public List<AuthorizationPolicyRule> getRules() {
        return rules;
    }

    public void setRules(List<AuthorizationPolicyRule> rules) {
        this.rules = rules;
    }

    public double getSamples() {
        return samples;
    }

    public void setSamples(double samples) {
        this.samples = samples;
    }

    public AuthorizationMatchType getMatchType() {
        return matchType;
    }

    public void setMatchType(AuthorizationMatchType matchType) {
        this.matchType = matchType;
    }

    public AuthorizationAction match(Endpoint peer, Endpoint local, Invocation invocation) {
        AuthorizationAction safeAction = this.action == null ? AuthorizationAction.ALLOW : this.action;
        if (rules == null || rules.isEmpty()) {
            return AuthorizationAction.ALLOW;
        }
        if (matchType == null || matchType == AuthorizationMatchType.ANY_MATCH) {
            for (AuthorizationPolicyRule rule : rules) {
                if (rule.match(peer, local, invocation)) {
                    return safeAction;
                }
            }
        } else {
            for (AuthorizationPolicyRule rule : rules) {
                if (!rule.match(peer, local, invocation)) {
                    return AuthorizationAction.ALLOW;
                }
            }
            return safeAction;
        }
        return AuthorizationAction.ALLOW;
    }
}
