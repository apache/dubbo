package com.alibaba.dubbo.demo;

/**
 * Created by wuyu on 2017/1/20.
 */
public class User {

    private String id;

    private String name;

    public User(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public User() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
