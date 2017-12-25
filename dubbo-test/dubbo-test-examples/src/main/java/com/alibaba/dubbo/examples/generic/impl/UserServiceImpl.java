package com.alibaba.dubbo.examples.generic.impl;

import com.alibaba.dubbo.examples.generic.api.IUserService;

public class UserServiceImpl implements IUserService {

    public User get(Params params) {
        return new User(1, "charles");
    }
}
