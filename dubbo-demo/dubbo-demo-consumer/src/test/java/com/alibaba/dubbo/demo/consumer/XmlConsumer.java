package com.alibaba.dubbo.demo.consumer;

import com.alibaba.dubbo.demo.User;
import com.alibaba.dubbo.demo.XmlService;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created by wuyu on 2017/2/5.
 */
public class XmlConsumer {
    public static void main(String[] args) {
        ClassPathXmlApplicationContext ctx=new ClassPathXmlApplicationContext("classpath:META-INF/spring/dubbo-demo-consumer.xml");
        XmlService xmlService = ctx.getBean(XmlService.class);
        String wuyu = xmlService.sayHello("wuyu");
        System.err.println(wuyu);

        User user = xmlService.getById("1");
        System.err.println(user);

    }
}
