package org.apache.dubbo.config.spring.issues.issue6000.adubbo;

import org.apache.dubbo.config.spring.api.HelloService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class HelloDubbo {
    HelloService helloService;
    @Resource
    public void setHelloService(HelloService helloService) {
        this.helloService = helloService;
    }

    public String sayHello(String name) {
        return helloService.sayHello(name);
    }
}