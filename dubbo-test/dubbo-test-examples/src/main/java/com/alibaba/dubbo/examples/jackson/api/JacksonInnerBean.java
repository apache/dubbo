package com.alibaba.dubbo.examples.jackson.api;

/**
 * Created by dylan on 11/15/14.
 */
public class JacksonInnerBean {
    private String siValue = "innerStr";
    private int iiValue = 18;

    public String getSiValue() {
        return siValue;
    }

    public void setSiValue(String siValue) {
        this.siValue = siValue;
    }

    public int getIiValue() {
        return iiValue;
    }

    public void setIiValue(int iiValue) {
        this.iiValue = iiValue;
    }

    @Override
    public String toString() {
        return "InnerBean{" +
                "siValue='" + siValue + '\'' +
                ", iiValue=" + iiValue +
                '}';
    }
}
