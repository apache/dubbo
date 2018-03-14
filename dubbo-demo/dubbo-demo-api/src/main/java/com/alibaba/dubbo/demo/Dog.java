package com.alibaba.dubbo.demo;

import java.io.Serializable;

public class Dog implements Serializable {

    private Integer age;

    public Integer getAge() {
        return age;
    }

    public Dog setAge(Integer age) {
        this.age = age;
        return this;
    }

}
