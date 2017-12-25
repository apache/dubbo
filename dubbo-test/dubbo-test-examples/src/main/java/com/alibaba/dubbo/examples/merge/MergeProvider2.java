package com.alibaba.dubbo.examples.merge;

import org.springframework.context.support.ClassPathXmlApplicationContext;


public class MergeProvider2 {

    public static void main(String[] args) throws Exception {
        String config = MergeProvider2.class.getPackage().getName().replace('.', '/') + "/merge-provider2.xml";
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(config);
        context.start();
        System.in.read();
    }

}
