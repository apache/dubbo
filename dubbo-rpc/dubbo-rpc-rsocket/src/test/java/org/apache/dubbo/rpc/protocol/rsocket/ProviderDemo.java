package org.apache.dubbo.rpc.protocol.rsocket;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ProviderDemo {

    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"spring/dubbo-rsocket-provider.xml"});
        context.start();
        System.in.read(); // press any key to exit
    }

}
