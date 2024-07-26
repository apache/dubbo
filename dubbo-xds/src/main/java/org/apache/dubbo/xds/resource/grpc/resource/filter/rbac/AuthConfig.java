package org.apache.dubbo.xds.resource.grpc.resource.filter.rbac;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class AuthConfig {

    private final List<PolicyMatcher> policies;

    private final Action action;

    public static AuthConfig create(List<PolicyMatcher> policies, Action action) {
        return new AuthConfig(policies, action);
    }

    AuthConfig(
            List<PolicyMatcher> policies, Action action) {
        if (policies == null) {
            throw new NullPointerException("Null policies");
        }
        this.policies = Collections.unmodifiableList(new ArrayList<>(policies));
        if (action == null) {
            throw new NullPointerException("Null action");
        }
        this.action = action;
    }

    public List<PolicyMatcher> policies() {
        return policies;
    }

    public Action action() {
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
            return this.policies.equals(that.policies()) && this.action.equals(that.action());
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
