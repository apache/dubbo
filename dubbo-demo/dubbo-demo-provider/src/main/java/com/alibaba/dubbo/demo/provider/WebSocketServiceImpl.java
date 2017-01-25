package com.alibaba.dubbo.demo.provider;

import com.alibaba.dubbo.demo.User;
import com.alibaba.dubbo.demo.WebSocketService;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by wuyu on 2017/1/19.
 */
public class WebSocketServiceImpl implements WebSocketService {
    @Override
    public String sayHello(String name) {
        return "Hello " + name;
    }

    @Override
    public User getById(String id) {
        return new User("1","wuyu");
    }

    @Override
    public List<User> listUser() {
        return Arrays.asList(new User("1","wuyu"),new User("2","zhangsan"));
    }

    @Override
    public Future<String> asyncSayHello(final String name) {
        return new Future<String>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return true;
            }

            @Override
            public String get() throws InterruptedException, ExecutionException {
                return "Hello " + name;
            }

            @Override
            public String get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return null;
            }
        };
    }

    @Override
    public Observable<String> rxSayHello(String name) {
        return Observable.just("Hello1 "+name,"Hello2 "+name);
    }
}
