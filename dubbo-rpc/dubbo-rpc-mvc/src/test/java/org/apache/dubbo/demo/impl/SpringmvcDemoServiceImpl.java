package org.apache.dubbo.demo.impl;

import org.apache.dubbo.demo.api.SpringmvcDemoService;

public class SpringmvcDemoServiceImpl implements SpringmvcDemoService {
    @Override
    public Integer hello(Integer a, Integer b) {
        return a+b;
    }

    @Override
    public String error() {
        return "error";
    }

    @Override
    public String sayHello(String name) {
        return name;
    }

    @Override
    public String getRemoteApplicationName() {
        return "test";
    }
}
