package com.alibaba.dubbo.examples.version;

import com.alibaba.dubbo.examples.version.api.VersionService;

import org.springframework.context.support.ClassPathXmlApplicationContext;


public class VersionConsumer {

    public static void main(String[] args) throws Exception {
        String config = VersionConsumer.class.getPackage().getName().replace('.', '/') + "/version-consumer.xml";
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(config);
        context.start();
        VersionService versionService = (VersionService) context.getBean("versionService");
        for (int i = 0; i < 10000; i++) {
            String hello = versionService.sayHello("world");
            System.out.println(hello);
            Thread.sleep(2000);
        }
        System.in.read();
    }

}
