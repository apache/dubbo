package com.alibaba.dubbo.common.threadpool.support.enhanced;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;

/**
 * enhanced task queue
 */
public class EnhancedTaskQueue<R extends Runnable> extends LinkedBlockingQueue<Runnable> {

    private static final long serialVersionUID = -2635853580887179627L;

    private EnhancedThreadPoolExecutor executor;

    public EnhancedTaskQueue(int capacity) {
        super(capacity);
    }

    public void setExecutor(EnhancedThreadPoolExecutor exec) {
        executor = exec;
    }

    @Override
    public boolean offer(Runnable runnable) {
        if (executor == null) {
            throw new RejectedExecutionException("enhanced queue does not have executor !");
        }
        int currentPoolThreadSize = executor.getPoolSize();
        //have free worker. put task into queue to let the worker deal with task.
        if (executor.getSubmittedTaskCount() < currentPoolThreadSize) {
            return super.offer(runnable);
        }

        // return false to let executor create new worker.
        if (currentPoolThreadSize < executor.getMaximumPoolSize()) {
            return false;
        }

        //currentPoolThreadSize >= max
        return super.offer(runnable);
    }

    /**
     * retry offer task
     *
     * @param o task
     * @return offer success or not
     * @throws RejectedExecutionException if executor is terminated.
     */
    public boolean retryOffer(Runnable o) {
        if (executor.isShutdown()) {
            throw new RejectedExecutionException("Executor is shutdown !");
        }
        return super.offer(o);
    }
}