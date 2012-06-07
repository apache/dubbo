package com.alibaba.dubbo.rpc.benchmark;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class RpcBenchmarkServer extends AbstractBenchmarkServer {

    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("ProviderSample.xml");
        ctx.start();
        synchronized (RpcBenchmarkServer.class) {
            try {
                RpcBenchmarkServer.class.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
