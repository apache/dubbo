package org.apache.dubbo.demo;

import java.io.Serializable;

public class MEntity implements Serializable {
    String a;
    Integer b;

    public Integer getB() {
        return b;
    }

    public void setB(Integer b) {
        this.b = b;
    }

    public String getA() {
        return a;
    }

    public void setA(String a) {
        this.a = a;
    }
}
