package org.apache.dubbo.common.threadpool.support.loom;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.threadpool.ThreadPool;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_THREAD_NAME;
import static org.apache.dubbo.common.constants.CommonConstants.THREAD_NAME_KEY;

/**
 * Creates a thread pool that use virtual thread
 *
 * @see Executors#newVirtualThreadPerTaskExecutor()
 */
public class VirtualThreadPool implements ThreadPool {
    @Override
    public Executor getExecutor(URL url) {
        String name = url.getParameter(THREAD_NAME_KEY, (String) url.getAttribute(THREAD_NAME_KEY, DEFAULT_THREAD_NAME));
        return Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual()
                .name(name, 1)
                .factory());
    }
}
