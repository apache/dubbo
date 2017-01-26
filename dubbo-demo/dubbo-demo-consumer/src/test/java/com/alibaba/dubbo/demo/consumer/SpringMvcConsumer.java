package com.alibaba.dubbo.demo.consumer;

import com.alibaba.dubbo.demo.UserService;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created by wuyu on 2017/1/10.
 */
public class SpringMvcConsumer {
    public static void main(String[] args) {

        ClassPathXmlApplicationContext ctx=new ClassPathXmlApplicationContext("classpath:META-INF/spring/dubbo-demo-consumer.xml");
        UserService userService = ctx.getBean(UserService.class);
        System.err.println(userService.sayHello("hello"));
    }
}
