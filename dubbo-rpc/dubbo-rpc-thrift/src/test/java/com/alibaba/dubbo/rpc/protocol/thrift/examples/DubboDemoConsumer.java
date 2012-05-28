package com.alibaba.dubbo.rpc.protocol.thrift.examples;

import com.alibaba.dubbo.rpc.gen.dubbo.Demo;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class DubboDemoConsumer {

    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext context =
                new ClassPathXmlApplicationContext("dubbo-demo-consumer.xml");
        context.start();
        Demo demo = (Demo) context.getBean("demoService");
        System.out.println(demo.echoI32(32));
        for (int i = 0; i < 10; i++) {
            System.out.println(demo.echoI32(i + 1));
        }
        context.close();
    }

}
