package com.alibaba.dubbo.demo.provider;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;

/**
 * Created by listening on 2017/3/7.
 */
public class Main {
    public static void main(String[] args) throws IOException {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("META-INF/spring/dubbo-demo-provider.xml");
        context.start();

        System.in.read();
    }
}
