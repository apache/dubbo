package com.alibaba.dubbo.common.threadpool.support.enhanced;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.threadpool.ThreadPool;
import com.alibaba.dubbo.common.threadpool.support.AbortPolicyWithReport;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;

import java.util.concurrent.*;

/**
 * enhanced thread pool.
 * When the core threads are all in busy , create new thread instead of putting task into blocking queue .
 */
public class EnhancedThreadPool implements ThreadPool {

    @Override
    public Executor getExecutor(URL url) {
        String name = url.getParameter(Constants.THREAD_NAME_KEY, Constants.DEFAULT_THREAD_NAME);
        int cores = url.getParameter(Constants.CORE_THREADS_KEY, Constants.DEFAULT_CORE_THREADS);
        int threads = url.getParameter(Constants.THREADS_KEY, Integer.MAX_VALUE);
        int queues = url.getParameter(Constants.QUEUES_KEY, Constants.DEFAULT_QUEUES);
        int alive = url.getParameter(Constants.ALIVE_KEY, Constants.DEFAULT_ALIVE);

        //init queue and enhanced executor
        EnhancedTaskQueue<Runnable> enhancedTaskQueue = new EnhancedTaskQueue<Runnable>(queues <= 0 ? 1 : queues);
        EnhancedThreadPoolExecutor executor = new EnhancedThreadPoolExecutor(cores, threads, alive, TimeUnit.MILLISECONDS, enhancedTaskQueue,
                new NamedThreadFactory(name, true), new AbortPolicyWithReport(name, url));
        enhancedTaskQueue.setExecutor(executor);
        return executor;
    }
}
