package com.alibaba.dubbo.demo.consumer;

import com.alibaba.dubbo.demo.CommentService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created by wuyu on 2017/1/17.
 */
public class JerseyConsumer {
    public static void main(String[] args) {
        ClassPathXmlApplicationContext ctx=new ClassPathXmlApplicationContext("classpath:META-INF/spring/dubbo-demo-consumer.xml");
        CommentService commentService = ctx.getBean(CommentService.class);
        JSONObject wuyu = commentService.sayHello("wuyu");
        System.err.println(wuyu);
    }
}
