package org.apache.dubbo.rpc.protocol.rsocket;

import org.apache.dubbo.rpc.service.DemoService;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

public class ConsumerDemo {

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"spring/dubbo-rsocket-consumer.xml"});
        context.start();
        DemoService demoService = (DemoService) context.getBean("demoService"); // get remote service proxy

        while (true) {
            try {
                Thread.sleep(1000);
                Mono<String> resultMono = demoService.requestMono("world"); // call remote method
                resultMono.doOnNext(new Consumer<String>() {
                    @Override
                    public void accept(String s) {
                        System.out.println(s); // get result
                    }
                }).block();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }


        }

    }

}
