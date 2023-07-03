package org.apache.dubbo.spring.boot.interceptor;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.spring.boot.toolkit.CallableWrapper;
import org.apache.dubbo.spring.boot.toolkit.RunnableWrapper;
import org.junit.Assert;
import org.junit.Test;

public class DubboCrossThreadTest {
    @Test
    public void crossThreadCallableTest() throws ExecutionException, InterruptedException, TimeoutException {
        Instrumentation instrumentation = ByteBuddyAgent.install();
        RunnableOrCallableActivation.install(instrumentation);
        String tag = "beta";
        RpcContext.getClientAttachment().setAttachment(CommonConstants.TAG_KEY, tag);
        Callable<String> callable = CallableWrapper.of(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return RpcContext.getClientAttachment().getAttachment(CommonConstants.TAG_KEY);
            }
        });
        ExecutorService threadPool = Executors.newSingleThreadExecutor();
        Future<String> submit = threadPool.submit(callable);
        Assert.assertEquals(tag, submit.get(1, TimeUnit.SECONDS));
        threadPool.shutdown();
    }

    private volatile String tagCrossThread = null;

    @Test
    public void crossThreadRunnableTest() throws ExecutionException, InterruptedException {
        Instrumentation instrumentation = ByteBuddyAgent.install();
        RunnableOrCallableActivation.install(instrumentation);
        String tag = "beta";
        RpcContext.getClientAttachment().setAttachment(CommonConstants.TAG_KEY, tag);
        final CountDownLatch latch = new CountDownLatch(1);
        Runnable runnable = RunnableWrapper.of(new Runnable() {
            @Override
            public void run() {
                String tag = RpcContext.getClientAttachment().getAttachment(CommonConstants.TAG_KEY);
                tagCrossThread = tag;
                latch.countDown();
            }
        });
        ExecutorService threadPool = Executors.newSingleThreadExecutor();
        threadPool.submit(runnable);
        latch.await(1, TimeUnit.SECONDS);
        Assert.assertEquals(tag, tagCrossThread);
        threadPool.shutdown();
    }

}
