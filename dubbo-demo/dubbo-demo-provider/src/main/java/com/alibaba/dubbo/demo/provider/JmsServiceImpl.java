package com.alibaba.dubbo.demo.provider;

import com.alibaba.dubbo.demo.JmsService;
import com.alibaba.dubbo.demo.User;

/**
 * Created by wuyu on 2017/2/5.
 */
public class JmsServiceImpl implements JmsService {
    @Override
    public String sayHello(String name) {
        return "Hello " + name;
    }

    @Override
    public User insert(User user) {
        return user;
    }
}
