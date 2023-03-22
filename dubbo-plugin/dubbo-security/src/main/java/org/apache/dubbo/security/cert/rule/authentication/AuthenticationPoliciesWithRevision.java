package org.apache.dubbo.security.cert.rule.authentication;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AuthenticationPoliciesWithRevision {
    private final List<AuthenticationPolicy> authenticationPolicies;
    private final long revision;

    private AuthenticationPoliciesWithRevision(List<AuthenticationPolicy> authenticationPolicies, long revision) {
        this.authenticationPolicies = authenticationPolicies;
        this.revision = revision;
    }

    public static AuthenticationPoliciesWithRevision of(String rawRule, long revision) {
        List<AuthenticationPolicy> authenticationPolicies = AuthenticationPolicy.parse(rawRule)
            .stream()
            .filter(p -> Objects.nonNull(p.getSpec()))
            .collect(Collectors.toList());
        return new AuthenticationPoliciesWithRevision(authenticationPolicies, revision);
    }

    public List<AuthenticationPolicy> getAuthenticationPolicies() {
        return authenticationPolicies;
    }

    public long getRevision() {
        return revision;
    }
}
