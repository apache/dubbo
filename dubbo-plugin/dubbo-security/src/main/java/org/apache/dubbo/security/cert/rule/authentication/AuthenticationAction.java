package org.apache.dubbo.security.cert.rule.authentication;

import org.apache.dubbo.common.ssl.AuthPolicy;

public enum AuthenticationAction {
    NONE,
    DISABLED,
    PERMISSIVE,
    STRICT;

    public AuthPolicy toAuthPolicy() {
        switch (this) {
            case DISABLED:
                return AuthPolicy.DISABLED;
            case PERMISSIVE:
                return AuthPolicy.PERMISSIVE;
            case STRICT:
                return AuthPolicy.STRICT;
            default:
                return AuthPolicy.NONE;
        }
    }
}
