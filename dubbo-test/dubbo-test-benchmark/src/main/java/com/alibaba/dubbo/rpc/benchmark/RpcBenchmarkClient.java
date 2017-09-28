package com.alibaba.dubbo.rpc.benchmark;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

public class RpcBenchmarkClient extends AbstractBenchmarkClient {

    public static void main(String[] args) throws Exception {
        new RpcBenchmarkClient().run(args);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public ClientRunnable getClientRunnable(String targetIP, int targetPort, int clientNums, int rpcTimeout,
                                            CyclicBarrier barrier, CountDownLatch latch, long startTime, long endTime) throws IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException {
        String runnable = properties.getProperty("classname");
        Class[] parameterTypes = new Class[]{String.class, int.class, int.class, int.class, CyclicBarrier.class,
                CountDownLatch.class, long.class, long.class};
        Object[] parameters = new Object[]{targetIP, targetPort, clientNums, rpcTimeout, barrier, latch, startTime,
                endTime};
        return (ClientRunnable) Class.forName(runnable).getConstructor(parameterTypes).newInstance(parameters);
    }
}
