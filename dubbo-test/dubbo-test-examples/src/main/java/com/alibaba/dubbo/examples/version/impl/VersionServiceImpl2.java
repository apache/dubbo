package com.alibaba.dubbo.examples.version.impl;

import com.alibaba.dubbo.examples.version.api.VersionService;


public class VersionServiceImpl2 implements VersionService {

    public String sayHello(String name) {
        return "hello2, " + name;
    }

}
