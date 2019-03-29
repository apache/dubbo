package io.github.kimmking.dubbo.rpc.protocol.xmlrpc;

/**
 * Created by kimmking(kimmking@163.com) on 2018/3/28.
 */
public interface XmlRpcService {
    String sayHello(String name);

    void timeOut(int millis);

    String customException();
}
