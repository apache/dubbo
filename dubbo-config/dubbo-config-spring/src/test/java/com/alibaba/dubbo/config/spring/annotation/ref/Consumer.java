package com.alibaba.dubbo.config.spring.annotation.ref;

import com.alibaba.dubbo.config.spring.ConfigTest;
import com.alibaba.dubbo.config.spring.annotation.consumer.AnnotationAction;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author cvictory ON 2018/10/16
 */
public class Consumer {

    public static void main(String[] args) {
        //Prevent to get IPV6 address,this way only work in debug mode
        //But you can pass use -Djava.net.preferIPv4Stack=true,then it work well whether in debug mode or not
        System.setProperty("java.net.preferIPv4Stack", "true");
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(ConfigTest.class.getPackage().getName().replace('.', '/') + "/annotation-reference-consumer.xml");
        context.start();
        AnnotationService annotationService = (AnnotationService) context.getBean(AnnotationService.class); // get remote service proxy

        while (true) {
            try {
                Thread.sleep(1000);
                annotationService.doSayName("ttt test");

            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }
}
