package com.alibaba.dubbo.examples.jackson.api;

/**
 * Created by dylan on 14-11-22.
 */
public abstract class AbstractInheritBean implements Inherit{
    private String username = "Dylan";
    private int age = 10;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    @Override
    public String toString() {
        return "AbstractInheritBean{" +
                "username='" + username + '\'' +
                ", age=" + age +
                '}';
    }
}
