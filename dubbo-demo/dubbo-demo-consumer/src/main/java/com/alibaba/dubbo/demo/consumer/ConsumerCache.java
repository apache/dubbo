package com.alibaba.dubbo.demo.consumer;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.alibaba.dubbo.demo.DemoBean;
import com.alibaba.dubbo.demo.DemoCacheService;

public class ConsumerCache {
	
	public static void main(String[] args) {
        //Prevent to get IPV6 address,this way only work in debug mode
        //But you can pass use -Djava.net.preferIPv4Stack=true,then it work well whether in debug mode or not
        System.setProperty("java.net.preferIPv4Stack", "true");
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/dubbo-demo-cache-consumer.xml"});
        context.start();
        DemoCacheService demoCacheService = (DemoCacheService) context.getBean("demoCacheService"); // get remote service proxy

        while (true) {
            try {
                /*String hello = demoService.sayHello("world"); // call remote method
                System.out.println(hello); // get result
                */
                DemoBean bean = demoCacheService.getBeanById(100);
                System.out.println(bean.getName());
                
                bean = demoCacheService.getBean(bean);
                System.out.println(bean.getName());
                
                bean.setName("name by consumer");
                demoCacheService.setBean(bean);
                
                bean = demoCacheService.getBean(bean);
                System.out.println(bean.getName());
                

                Thread.sleep(100000);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }


        }

    }

}
