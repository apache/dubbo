package com.alibaba.dubbo.examples.jackson.api;

import org.joda.time.DateTime;

/**
 * Created by dylan on 14-11-22.
 */
public class InheritBean2 extends AbstractInheritBean {
    private String zipCode = "200000";

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    @Override
    public String toString() {
        return "InheritBean2{" +
                "zipCode='" + zipCode + '\'' +
                "} " + super.toString();
    }
}
