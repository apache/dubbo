package com.alibaba.dubbo.demo;


import rx.Observable;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * Created by wuyu on 2017/1/19.
 */
public interface WebSocketService {

    public String sayHello(String name);

    public User getById(String id);

    public List<User> listUser();

    public User insert(User user);

    public Future<String> asyncSayHello(String name);

    public Observable<String> rxSayHello(String name);

    public Set<String> getAllClientSessionId();

    public Set<String> getAllRoom();

    public void sendBroadcastMessage(String message);

    public Set<String> getAllClientRemoteSocketAddress();

}
