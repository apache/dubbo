package org.apache.dubbo.security.cert.rule;

import org.apache.dubbo.rpc.Invocation;

public class AuthorizationCondition {
    private String key;
    private AuthorizationMatch values;
    private AuthorizationMatch notValues;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public AuthorizationMatch getValues() {
        return values;
    }

    public void setValues(AuthorizationMatch values) {
        this.values = values;
    }

    public AuthorizationMatch getNotValues() {
        return notValues;
    }

    public void setNotValues(AuthorizationMatch notValues) {
        this.notValues = notValues;
    }

    public boolean match(Invocation invocation) {
        if (key == null) {
            return true;
        }

        // TODO parameter matching
        return true;
    }
}
