package com.alibaba.dubbo.registry.common.domain;

import java.io.Serializable;

public class Dependency implements Serializable {

    private static final long serialVersionUID = 8526869025719540547L;

    private String providerApplication;

    private String consumerApplication;

    public String getProviderApplication() {
        return providerApplication;
    }

    public void setProviderApplication(String providerApplication) {
        this.providerApplication = providerApplication;
    }

    public String getConsumerApplication() {
        return consumerApplication;
    }

    public void setConsumerApplication(String consumerApplication) {
        this.consumerApplication = consumerApplication;
    }

}
