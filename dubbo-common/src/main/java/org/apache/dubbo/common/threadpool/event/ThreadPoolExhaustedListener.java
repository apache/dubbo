package org.apache.dubbo.common.threadpool.event;

public interface ThreadPoolExhaustedListener {

    /**
     * Notify when the thread pool is exhausted.
     * {@link org.apache.dubbo.common.threadpool.support.AbortPolicyWithReport}
     */
    void onEvent(ThreadPoolExhaustedEvent event);
}
