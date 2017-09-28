package com.alibaba.dubbo.rpc.benchmark;

public class BenchmarkServer extends AbstractBenchmarkServer {

    public static void main(String[] args) throws Exception {
        new BenchmarkServer().run(args);
        synchronized (BenchmarkServer.class) {
            BenchmarkServer.class.wait();
        }
    }
}
