package org.apache.dubbo.common.threadpool;

import org.apache.dubbo.common.threadpool.affinity.AbstractKeyAffinityExecutor;
import org.apache.dubbo.common.threadpool.affinity.KeyAffinityExecutor;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class RandomKeyAffinityExecutorTest {

    private static KeyAffinityExecutor<String> keyAffinityExecutor;

    @BeforeAll
    public static void before() {
        keyAffinityExecutor = AbstractKeyAffinityExecutor.newRandomAffinityExecutor();
    }

    @Test
    void test1() {
        for (int i = 0; i < 1000; i++) {
            final String val = i % 5 + "";
            keyAffinityExecutor.execute(val, () -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("thread name=" + Thread.currentThread().getName() + " " + val);
            });
        }
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < 1000; i++) {
            final String val = i % 5 + "";
            keyAffinityExecutor.execute(val, () -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("thread name=" + Thread.currentThread().getName() + " " + val);
            });
        }
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    @AfterAll
    public static void after() {
        keyAffinityExecutor.destroyAll();
    }


}