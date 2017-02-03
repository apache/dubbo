package com.alibaba.dubbo.demo.consumer;

import com.alibaba.dubbo.demo.HproseService;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created by wuyu on 2017/2/3.
 */
public class HproseConsumer {
    public static void main(String[] args) {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:META-INF/spring/dubbo-demo-consumer.xml");
        HproseService hproseService = ctx.getBean(HproseService.class);
        String wuyu = hproseService.sayHello("wuyu");
        System.err.println(wuyu);
    }
}
