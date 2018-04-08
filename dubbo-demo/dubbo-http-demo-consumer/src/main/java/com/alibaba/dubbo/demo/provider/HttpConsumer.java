package com.alibaba.dubbo.demo.provider;

import com.alibaba.dubbo.demo.HttpDemoService;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class HttpConsumer {

    public static void main(String[] args) {
        System.setProperty("java.net.preferIPv4Stack", "true");
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/dubbo-demo-consumer.xml"});
        context.start();
        HttpDemoService demoService = (HttpDemoService) context.getBean("demoService"); // get remote service proxy

        while (true) {
            try {
                Thread.sleep(1000);

                String hello = demoService.hello("world"); // call remote method
                System.out.println(hello); // get result


            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }


        }
    }

}
