package com.alibaba.dubbo.config;

class GreetingLocal2 implements Greeting {
    @Override
    public String hello() {
        return "local";
    }
}
