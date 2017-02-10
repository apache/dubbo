package com.alibaba.dubbo.demo.provider;

import com.alibaba.dubbo.demo.Redis2Service;
import com.alibaba.dubbo.demo.User;

/**
 * Created by wuyu on 2017/2/7.
 */
public class Redis2ServiceImpl implements Redis2Service {
    @Override
    public String sayHello(String name) {
        return "Hello " + name;
    }

    @Override
    public Integer sum(int i, int j) {
        return i + j;
    }

    @Override
    public User getById(String id) {
        return new User(id,"wuyu");
    }

    @Override
    public void insert(String id) {
        System.err.println(id);
    }
}
