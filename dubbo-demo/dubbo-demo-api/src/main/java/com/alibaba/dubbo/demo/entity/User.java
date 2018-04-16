package com.alibaba.dubbo.demo.entity;

import com.alibaba.dubbo.demo.DemoService;

import javax.validation.constraints.NotNull;

public class User {

    @NotNull(message = "用户名不能为空")
    private String username;
    @NotNull(message = "密码不能为空", groups = DemoService.Save.class)
    private String password;

    public String getUsername() {
        return username;
    }

    public User setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public User setPassword(String password) {
        this.password = password;
        return this;
    }

}