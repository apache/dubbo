package com.alibaba.dubbo.rpc.benchmark;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

public class BenchmarkClient extends AbstractBenchmarkClient {

    @Override
    public ClientRunnable getClientRunnable(String targetIP, int targetPort, int clientNums, int rpcTimeout,
                                            CyclicBarrier barrier,
                                            CountDownLatch latch, long endTime, long startTime) {
        return new SimpleProcessorBenchmarkClientRunnable(targetIP, targetPort, clientNums, rpcTimeout,
                                                         barrier, latch, startTime, endTime);
    }

    public static void main(String[] args) throws Exception {
        new BenchmarkClient().run(args);
    }
}
