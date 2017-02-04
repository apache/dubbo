package com.alibaba.dubbo.demo.provider;

import com.alibaba.dubbo.demo.User;
import com.alibaba.dubbo.demo.XmlService;

/**
 * Created by wuyu on 2017/2/5.
 */
public class XmlServiceImpl implements XmlService {
    @Override
    public String sayHello(String name) {
        return "Hello " + name;
    }

    @Override
    public User getById(String id) {
        return new User(id,"wuyu");
    }
}
