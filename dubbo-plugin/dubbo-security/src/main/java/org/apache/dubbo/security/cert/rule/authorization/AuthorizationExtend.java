package org.apache.dubbo.security.cert.rule.authorization;

import org.apache.dubbo.security.cert.Endpoint;

public class AuthorizationExtend {
    private String key;
    private String value;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean match(Endpoint endpoint) {
        if (key == null) {
            return true;
        }

        String valueFromUrl = endpoint.getParameter(key);
        if (valueFromUrl == null) {
            return false;
        }

        return value == null || value.equals(valueFromUrl);
    }
}
