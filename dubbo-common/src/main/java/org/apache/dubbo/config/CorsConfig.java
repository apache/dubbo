package org.apache.dubbo.config;

import java.io.Serializable;

public class CorsConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private String allowedOrigins;

    private String allowedMethods;

    private String allowedHeaders;

    private String exposedHeaders;

    private Boolean allowCredentials;

    private Boolean allowPrivateNetWork;


    private Long maxAge;

    public String getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(String allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public String getAllowedMethods() {
        return allowedMethods;
    }

    public void setAllowedMethods(String allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    public String getAllowedHeaders() {
        return allowedHeaders;
    }

    public void setAllowedHeaders(String allowedHeaders) {
        this.allowedHeaders = allowedHeaders;
    }

    public String getExposedHeaders() {
        return exposedHeaders;
    }

    public void setExposedHeaders(String exposedHeaders) {
        this.exposedHeaders = exposedHeaders;
    }

    public Boolean getAllowCredentials() {
        return allowCredentials;
    }

    public void setAllowCredentials(Boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }

    public Long getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(Long maxAge) {
        this.maxAge = maxAge;
    }

    public Boolean getAllowPrivateNetWork() {
        return allowPrivateNetWork;
    }

    public void setAllowPrivateNetWork(Boolean allowPrivateNetWork) {
        this.allowPrivateNetWork = allowPrivateNetWork;
    }
}
