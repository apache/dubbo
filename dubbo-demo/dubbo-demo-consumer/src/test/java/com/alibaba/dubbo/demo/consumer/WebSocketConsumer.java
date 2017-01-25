package com.alibaba.dubbo.demo.consumer;

import com.alibaba.dubbo.demo.User;
import com.alibaba.dubbo.demo.WebSocketService;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import rx.Observable;
import rx.Subscriber;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by wuyu on 2017/1/19.
 */
public class WebSocketConsumer {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:META-INF/spring/dubbo-demo-consumer.xml");
        final WebSocketService webSocketService = ctx.getBean(WebSocketService.class);

        User user = webSocketService.getById("1");
        System.err.println(user.toString());

        List<User> users = webSocketService.listUser();
        System.err.println(users.toString());

        Future<String> future = webSocketService.asyncSayHello("wuyu");
        System.err.println(future.get());

        Observable<String> rxSayHello = webSocketService.rxSayHello("wuyu");
        rxSayHello.subscribe(new Subscriber<String>() {
            @Override
            public void onCompleted() {
                System.err.println("rxSayHello 执行完成！");

            }

            @Override
            public void onError(Throwable e) {
                System.err.println(e.getMessage());
            }

            @Override
            public void onNext(String s) {
                System.err.println("rxSayHello :" + s);
            }
        });
    }
}
