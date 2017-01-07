package com.alibaba.dubbo.rpc.protocol.jsonrpc;

/**
 * Created by wuwen on 15/4/1.
 */
public interface JsonRpcService {
    String sayHello(String name);

    void timeOut(int millis);

    String customException();
}
