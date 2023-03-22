package org.apache.dubbo.security.cert.rule.authentication;

import org.apache.dubbo.common.utils.JsonUtils;

import java.util.List;

public class AuthenticationPolicy {
    private String name;
    private AuthenticationPolicySpec spec;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AuthenticationPolicySpec getSpec() {
        return spec;
    }

    public void setSpec(AuthenticationPolicySpec spec) {
        this.spec = spec;
    }

    public AuthenticationAction match(int port) {
        if (spec == null) {
            return null;
        }
        return spec.match(port);
    }

    public static List<AuthenticationPolicy> parse(String raw) {
        return JsonUtils.getJson().toJavaList(raw, AuthenticationPolicy.class);
    }
}
