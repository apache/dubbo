package org.apache.dubbo.security.cert.rule.authentication;

public class AuthenticationPolicyPortLevel {
    private int port;
    private AuthenticationAction action;

    public AuthenticationAction getAction() {
        return action;
    }

    public void setAction(AuthenticationAction action) {
        this.action = action;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public AuthenticationAction match(int port) {
        if (this.port == port) {
            return action;
        }
        return null;
    }
}
