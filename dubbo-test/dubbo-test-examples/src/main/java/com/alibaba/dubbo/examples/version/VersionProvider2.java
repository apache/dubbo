package com.alibaba.dubbo.examples.version;

import org.springframework.context.support.ClassPathXmlApplicationContext;


public class VersionProvider2 {

    public static void main(String[] args) throws Exception {
        String config = VersionProvider2.class.getPackage().getName().replace('.', '/') + "/version-provider2.xml";
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(config);
        context.start();
        System.in.read();
    }

}
