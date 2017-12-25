package com.alibaba.dubbo.examples.merge;

import org.springframework.context.support.ClassPathXmlApplicationContext;


public class MergeProvider {

    public static void main(String[] args) throws Exception {
        String config = MergeProvider.class.getPackage().getName().replace('.', '/') + "/merge-provider.xml";
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(config);
        context.start();
        System.in.read();
    }

}
