package com.apache.dubbo.demo.web.consumer.controller;

import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.demo.DemoService;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Weihua
 * @since 1.0.0
 */
@RestController
@RequestMapping
public class Controller {

    @DubboReference
    private DemoService demoService;

    @RequestMapping("hello/{name}")
    public String sayHello(@PathVariable String name){
        return demoService.sayHello(name);
    }
}
