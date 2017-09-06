package com.alibaba.dubbo.async;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by zhaohui.yu
 * 15/11/13
 */
public class AsyncContextImpl<T extends Serializable> implements AsyncContext<T> {
    private volatile Runnable runnable;

    private static final int INIT = 0;
    private static final int DONE = 1;
    private static final int EXECUTED = 2;

    private final AtomicInteger state = new AtomicInteger(0);

    private T result;

    private Throwable exception;

    @Override
    public void commit() {
        if (state.compareAndSet(INIT, DONE)) {
            trigger();
        }
    }

    @Override
    public void commit(T result) {
        this.result = result;
        if (state.compareAndSet(INIT, DONE)) {
            trigger();
        }
    }

    @Override
    public void fail(Throwable t) {
        this.exception = t;
        if (state.compareAndSet(INIT, DONE)) {
            trigger();
        }
    }

    public void then(Runnable runnable) {
        this.runnable = runnable;
        trigger();
    }

    protected final void trigger() {
        Runnable run = runnable;
        if (run == null) return;

        if (state.compareAndSet(DONE, EXECUTED)) {
            run.run();
        }
    }

    public Object getResult() {
        if (state.get() < DONE) return null;
        return this.result;
    }

    public Throwable getException() {
        if (state.get() < DONE) return null;
        return this.exception;
    }
}