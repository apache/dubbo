package com.alibaba.dubbo.rpc.protocol.avro;

import org.apache.avro.ipc.Responder;

/**
 * Created by wuyu on 2016/6/15.
 */
public class ResponderHolder {

    private static ThreadLocal<Responder> responderThreadLocal = new ThreadLocal<>();

    public static Responder getResponder() {
        return responderThreadLocal.get();
    }

    public static void setResponder(Responder responder) {
        responderThreadLocal.set(responder);
    }
}
