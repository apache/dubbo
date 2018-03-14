package com.alibaba.dubbo.demo;

import java.io.Serializable;

/**
 * 猫
 */
public class Cat implements Serializable {

    /**
     * 名字
     */
    private String name;

    public String getName() {
        return name;
    }

    public Cat setName(String name) {
        this.name = name;
        return this;
    }

}
