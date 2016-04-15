package com.alibaba.dubbo.examples.callback;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created by tanhua on 16/4/15.
 */
public class FooBarProvider {
    public static void main(String[] args) throws Exception {
        System.out.println("start service : " + args[0]);
        String config = CallbackProvider.class.getPackage().getName().replace('.', '/') + "/" + args[0] +"-provider.xml";
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(config);
        context.start();
        System.in.read();
    }
}
