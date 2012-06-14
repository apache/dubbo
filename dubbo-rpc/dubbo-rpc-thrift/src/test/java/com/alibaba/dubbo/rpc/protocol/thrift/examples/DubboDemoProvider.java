package com.alibaba.dubbo.rpc.protocol.thrift.examples;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class DubboDemoProvider {

    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext context =
                new ClassPathXmlApplicationContext("dubbo-demo-provider.xml");
        context.start();
        System.out.println("context started");
        System.in.read();
    }

}
