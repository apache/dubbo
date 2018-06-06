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

//        int currentPoolThreadSize = executor.getPoolSize();
//        //如果线程池里的线程数量已经到达最大,将任务添加到队列中
//        if (currentPoolThreadSize == executor.getMaximumPoolSize()) {
//            return super.offer(runnable);
//        }
//        //说明有空闲的线程,这个时候无需创建core线程之外的线程,而是把任务直接丢到队列里即可
//        if (executor.getSubmittedTaskCount() < currentPoolThreadSize) {
//            return super.offer(runnable);
//        }
//
//        //如果线程池里的线程数量还没有到达最大,直接创建线程,而不是把任务丢到队列里面
//        if (currentPoolThreadSize < executor.getMaximumPoolSize()) {
//            return false;
//        }
//
//        return super.offer(runnable);
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