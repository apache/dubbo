package com.alibaba.dubbo.demo.provider;

import com.alibaba.dubbo.demo.IUserService;
import com.alibaba.dubbo.demo.User;

public class UserService implements IUserService{
    @Override
    public User echo(User user) {
        return user;
    }
}
