package org.apache.dubbo.security.cert.rule;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.rpc.Invocation;

import java.util.List;

public class AuthorizationPolicy implements Rule {
    private String name;
    private AuthorizationPolicySpec spec;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AuthorizationPolicySpec getSpec() {
        return spec;
    }

    public void setSpec(AuthorizationPolicySpec spec) {
        this.spec = spec;
    }

    public AuthorizationAction match(URL peer, URL local, Invocation invocation) {
        if (spec == null) {
            return AuthorizationAction.ALLOW;
        }
        return spec.match(peer, local, invocation);
    }

    public static List<AuthorizationPolicy> parse(String raw) {
        return JsonUtils.getJson().toJavaList(raw, AuthorizationPolicy.class);
    }
}
