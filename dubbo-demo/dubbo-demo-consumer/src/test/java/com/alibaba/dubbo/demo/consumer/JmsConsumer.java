package com.alibaba.dubbo.demo.consumer;

import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.demo.JmsService;
import com.alibaba.dubbo.demo.User;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created by wuyu on 2017/2/5.
 */
public class JmsConsumer {
    public static void main(String[] args) {
        ConfigUtils.getProperties().put("brokerURL", "tcp://localhost:61616");
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:META-INF/spring/dubbo-demo-consumer.xml");
        JmsService jmsService = ctx.getBean(JmsService.class);
        String wuyu = jmsService.sayHello("wuyu");
        System.err.println(wuyu);
        User wuyu1 = jmsService.insert(new User("1", "wuyu"));
        System.err.println(wuyu1);
    }
}
