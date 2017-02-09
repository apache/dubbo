package com.alibaba.dubbo.demo.consumer;

import com.alibaba.dubbo.demo.HproseService;
import com.alibaba.dubbo.demo.Redis2Service;
import com.alibaba.dubbo.demo.User;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created by wuyu on 2017/2/3.
 */
public class Redis2ServiceConsumer {
    public static void main(String[] args) {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:META-INF/spring/dubbo-demo-consumer.xml");
        Redis2Service redis2Service = ctx.getBean(Redis2Service.class);
        String wuyu = redis2Service.sayHello("wuyu");
        System.err.println(wuyu);
        Integer sum = redis2Service.sum(1, 2);
        System.err.println(sum);
        User byId = redis2Service.getById("1");
        System.err.println(byId);
    }
}
