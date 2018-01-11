package com.alibaba.dubbo.common.threadpool.support.enhanced;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Enhanced thread pool
 */
public class EnhancedThreadPoolExecutor extends ThreadPoolExecutor {

    //task count
    private final AtomicInteger submittedTaskCount = new AtomicInteger(0);

    public EnhancedThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, EnhancedTaskQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    /**
     * @return current tasks which are executed
     */
    public int getSubmittedTaskCount() {
        return submittedTaskCount.get();
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        submittedTaskCount.decrementAndGet();
    }

    @Override
    public void execute(Runnable command) {
        //do not increment in method beforeExecute!
        submittedTaskCount.incrementAndGet();
        try {
            super.execute(command);
        } catch (RejectedExecutionException rx) {
            //retry to offer the task into queue .
            final EnhancedTaskQueue queue = (EnhancedTaskQueue<Runnable>) super.getQueue();
            if (!queue.retryOffer(command)) {
                submittedTaskCount.decrementAndGet();
                throw new RejectedExecutionException();
            }
        }
    }
}