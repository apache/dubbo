package com.alibaba.dubbo.demo.provider;

import com.alibaba.dubbo.demo.User;
import com.alibaba.dubbo.demo.WebSocketService;
import com.alibaba.dubbo.rpc.protocol.websocket.BroadcastMessage;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIONamespace;
import rx.Observable;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by wuyu on 2017/1/19.
 */
public class WebSocketServiceImpl implements WebSocketService {

    //自动注入
    private SocketIONamespace socketIONamespace;

    @Override
    public String sayHello(String name) {
        return "Hello " + name;
    }

    @Override
    public User getById(String id) {
        return new User("1", "wuyu");
    }

    @Override
    public List<User> listUser() {
        return Arrays.asList(new User("1", "wuyu"), new User("2", "zhangsan"));
    }

    @Override
    public User insert(User user) {
        return user;
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
                return "Hello " + name;
            }
        };
    }

    @Override
    public Observable<String> rxSayHello(String name) {
        return Observable.just("Hello1 " + name, "Hello2 " + name);
    }

    @Override
    public int error(String number) {
        return Integer.parseInt(number);
    }

    public Set<String> getAllClientSessionId() {
        Collection<SocketIOClient> allClients = socketIONamespace.getAllClients();
        Set<String> allClientsId = new HashSet<>();
        for (SocketIOClient client : allClients) {
            allClientsId.add(client.getSessionId().toString());
        }
        return allClientsId;
    }

    public Set<String> getAllClientRemoteSocketAddress() {
        Collection<SocketIOClient> allClients = socketIONamespace.getAllClients();
        Set<String> allClient = new HashSet<>();
        for (SocketIOClient client : allClients) {
            InetSocketAddress socketAddress = (InetSocketAddress) client.getRemoteAddress();
            allClient.add(socketAddress.toString());
        }
        return allClient;
    }

    public Set<String> getAllRoom() {
        Collection<SocketIOClient> allClients = socketIONamespace.getAllClients();
        Set<String> allClient = new HashSet<>();
        for (SocketIOClient client : allClients) {
            Set<String> allRooms = client.getAllRooms();
            allClient.addAll(allRooms);
        }
        return allClient;
    }

    //发送广播消息,订阅broadcast 通道的客户端将接受到 广播消息
    public void sendBroadcastMessage(String message) {
        socketIONamespace.getBroadcastOperations().sendEvent("broadcast", BroadcastMessage.newBuilder().setResult("这是一条广播消息!").build());
    }


}
