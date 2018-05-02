package com.alibaba.dubbo.config;

class GreetingLocal3 implements Greeting {
    private Greeting greeting;

    public GreetingLocal3(Greeting greeting) {
        this.greeting = greeting;
    }

    @Override
    public String hello() {
        return null;
    }
}
