package com.alibaba.dubbo.oauth2.property;

import java.util.Set;

/**
 * Created by wuyu on 2017/1/7.
 */
public class UserDetails {

    private String principal;

    private Set<String> authorities;


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
}
