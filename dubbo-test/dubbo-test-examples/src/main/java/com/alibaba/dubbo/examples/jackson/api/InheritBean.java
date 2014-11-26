package com.alibaba.dubbo.examples.jackson.api;

import java.util.Date;

/**
 * Created by dylan on 14-11-22.
 */
public class InheritBean extends AbstractInheritBean {
    private String address = "ShangHai";
    private Date birthDate = new Date();

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }

    @Override
    public String toString() {
        return "InheritBean{" +
                "address='" + address + '\'' +
                ", birthDate=" + birthDate +
                "} " + super.toString();
    }
}
