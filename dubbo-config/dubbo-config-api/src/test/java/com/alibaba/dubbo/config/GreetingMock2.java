package com.alibaba.dubbo.config;

public class GreetingMock2 implements Greeting {
    private GreetingMock2() {
    }

    @Override
    public String hello() {
        return "mock";
    }
}
