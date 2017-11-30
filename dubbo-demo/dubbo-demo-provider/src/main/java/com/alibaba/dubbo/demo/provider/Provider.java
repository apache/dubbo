package com.alibaba.dubbo.demo.provider;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created by ken.lj on 2017/7/31.
 */
public class Provider {

    public static void main(String[] args) throws Exception {
        System.setProperty("java.net.preferIPv4Stack", "true"); //防止无线网络自动返回IPv6地址
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/dubbo-demo-provider.xml"});
        context.start();

        System.in.read(); // 按任意键退出
    }

}
