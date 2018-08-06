package org.apache.dubbo.common.concurrent;

import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CompletableFutureTaskTest {

    @Test
    public void testCreate() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        CompletableFutureTask<Boolean> futureTask = CompletableFutureTask.create(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                countDownLatch.countDown();
                return true;
            }
        });
        futureTask.run();
        countDownLatch.await();
    }

    @Test
    public void testRunnableResponse() throws ExecutionException, InterruptedException {
        CompletableFutureTask<Boolean> futureTask = CompletableFutureTask.create(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, true);
        futureTask.run();

        Boolean result = futureTask.get();
        assertThat(result, is(true));
    }

    @Test
    public void testListener() throws InterruptedException {
        CompletableFutureTask<String> futureTask = CompletableFutureTask.create(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Thread.sleep(500);
                return "hello";
            }
        });
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        futureTask.addListener(new Runnable() {
            @Override
            public void run() {
                countDownLatch.countDown();
            }
        });
        futureTask.run();
        countDownLatch.await();
    }


    @Test
    public void testCustomExecutor() {
        Executor mockedExecutor = mock(Executor.class);
        CompletableFutureTask<Integer> futureTask = CompletableFutureTask.create(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return 0;
            }
        });
        futureTask.addListener(mock(Runnable.class), mockedExecutor);
        futureTask.run();

        verify(mockedExecutor).execute(any(Runnable.class));
    }
}