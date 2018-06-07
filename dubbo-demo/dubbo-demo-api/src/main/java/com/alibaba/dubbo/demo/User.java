package com.alibaba.dubbo.demo;


import java.io.Serializable;

public class User implements Serializable {
    private String name;
    private String mobile;
    private int age;

    public User(String name, String mobile, int age){
        this.name = name;
        this.mobile = mobile;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public String getMobile() {
        return mobile;
    }

    public int getAge() {
        return age;
    }
}
