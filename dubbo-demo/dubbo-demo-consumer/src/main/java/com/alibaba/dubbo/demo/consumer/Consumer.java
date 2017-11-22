package com.alibaba.dubbo.demo.consumer;

import com.alibaba.dubbo.demo.DemoService;

import com.alibaba.dubbo.rpc.RpcContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Date;

/**
 * Created by ken.lj on 2017/7/31.
 */
public class Consumer {

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/dubbo-demo-consumer.xml"});
        context.start();

        DemoService demoService = (DemoService) context.getBean("demoService"); // 获取远程服务代理
        RpcContext.getContext().setAttachment("userId", "123");
        String hello = demoService.sayHello("world", new Date()); // 执行远程方法

        System.out.println(hello); // 显示调用结果

        RpcContext.getContext().setAttachment("userId", "321");
        hello = demoService.sayHello("world", new Date(System.currentTimeMillis() - 1000000)); // 执行远程方法

        System.out.println(hello); // 显示调用结果
    }
}
