package com.alibaba.dubbo.demo.consumer;

import com.alibaba.dubbo.demo.FooService;
import com.alibaba.dubbo.demo.UserService;
import org.apache.thrift.TException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created by wuyu on 2017/1/17.
 */
public class Thirft9Consumer {
    public static void main(String[] args) throws TException {
        ClassPathXmlApplicationContext ctx=new ClassPathXmlApplicationContext("classpath:META-INF/spring/dubbo-demo-consumer.xml");
        FooService.Iface fooService = ctx.getBean(FooService.Iface.class);
        System.err.println(fooService.sayHello("wuyu"));
    }
}
