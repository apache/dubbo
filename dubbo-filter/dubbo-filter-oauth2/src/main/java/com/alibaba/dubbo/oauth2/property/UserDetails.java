package com.alibaba.dubbo.oauth2.property;

import java.util.Set;

/**
 * Created by wuyu on 2017/1/7.
 */
public class UserDetails {

    private String principal;

    private Set<String> authorities;

    private Set<String> scope;

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    public Set<String> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(Set<String> authorities) {
        this.authorities = authorities;
    }

    public Set<String> getScope() {
        return scope;
    }

    public void setScope(Set<String> scope) {
        this.scope = scope;
    }
}
