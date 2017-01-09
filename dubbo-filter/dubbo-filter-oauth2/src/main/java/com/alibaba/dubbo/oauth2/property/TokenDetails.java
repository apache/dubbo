package com.alibaba.dubbo.oauth2.property;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * Created by wuyu on 2016/12/29.
 */
public class TokenDetails {

    @JSONField(name = "access_token")
    private String accessToken;

    @JSONField(name = "token_type")
    private String tokenType;

    @JSONField(name = "scope")
    private String scope;

    @JSONField(name = "expires_in")
    private long expiresIn;

    private long createdTime = System.currentTimeMillis();

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }


    @Override
    public String toString() {
        return "TokenDetails{" +
                "accessToken='" + accessToken + '\'' +
                ", tokenType='" + tokenType + '\'' +
                ", scope='" + scope + '\'' +
                ", expiresIn=" + expiresIn +
                ", createdTime=" + createdTime +
                '}';
    }
}
