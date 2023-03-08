package org.apache.dubbo.security.cert.rule;

import org.apache.dubbo.common.URL;

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

    public boolean match(URL url) {
        if (key == null) {
            return true;
        }

        String valueFromUrl = url.getParameter(key);
        if (valueFromUrl == null) {
            return false;
        }

        return value == null || value.equals(valueFromUrl);
    }
}
