package org.apache.dubbo.metadata.store.zookeeper;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @author cvictory ON 2019-06-28
 */
public class Test {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        CompletableFuture<String> d = new CompletableFuture<String>();
        d.whenComplete((v, t) -> {
            System.out.println("v1 " + v + " , " + t);
        });
        CompletableFuture<String> dd = d.whenComplete((v, t) -> {
            System.out.println("v2 " + v + " , " + t);
            throw new RuntimeException();
        });
        dd.whenComplete((v, t) -> {
            System.out.println("v3 " + v + " , " + t);
        });
        d.complete("vvv");
        System.out.println(d.get());
        System.out.println(dd.get());
    }
}
