package org.apache.dubbo.common.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RunnableFuture;

public class CompletableFutureTask<V> extends CompletableFuture<V> implements RunnableFuture<V> {
    private final Callable<V> target;
    private final ExecutionList executionList = new ExecutionList();

    CompletableFutureTask(Callable<V> callable) {
        super();
        if(callable == null) {
            throw new NullPointerException();
        }
        this.target = callable;
    }

    public static <V> CompletableFutureTask<V> create(Callable<V> callable) {
        return new CompletableFutureTask(callable);
    }

    @Override
    public void run() {
        if(isCancelled())
            return;
        try {
            final V result = target.call();
            this.complete(result);
        } catch (Throwable ex) {
            this.completeExceptionally(ex);
        }
    }

    public void addListener(Runnable listener, Executor exec) {
        executionList.add(listener, exec);
    }

    public void addListener(Runnable listener) {
        executionList.add(listener, null);
    }

    @Override
    public boolean complete(V value) {
        executionList.execute();
        return super.complete(value);
    }
}
