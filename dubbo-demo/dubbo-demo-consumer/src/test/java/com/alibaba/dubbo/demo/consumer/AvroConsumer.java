package com.alibaba.dubbo.demo.consumer;

import com.alibaba.dubbo.demo.AvroService;
import org.apache.avro.AvroRemoteException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created by wuyu on 2017/1/18.
 */
public class AvroConsumer {
    public static void main(String[] args) throws AvroRemoteException {
        ClassPathXmlApplicationContext ctx=new ClassPathXmlApplicationContext("classpath:META-INF/spring/dubbo-demo-consumer.xml");
        AvroService avroService = ctx.getBean(AvroService.class);
        CharSequence wuyu = avroService.sayHello("wuyu");
        System.err.println(wuyu);

    }
}
