/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.common.threadpool.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.event.ThreadPoolExhaustedEvent;
import org.apache.dubbo.common.threadpool.event.ThreadPoolExhaustedListener;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.JVMUtil;
import org.apache.dubbo.common.utils.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;

import static java.lang.String.format;
import static org.apache.dubbo.common.constants.CommonConstants.DUMP_DIRECTORY;
import static org.apache.dubbo.common.constants.CommonConstants.OS_NAME_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.OS_WIN_PREFIX;

/**
 * Abort Policy.
 * Log warn info when abort.
 */
public class AbortPolicyWithReport extends ThreadPoolExecutor.AbortPolicy {

    protected static final Logger logger = LoggerFactory.getLogger(AbortPolicyWithReport.class);

    private final String threadName;

    private final URL url;

    private static volatile long lastPrintTime = 0;

    private static final long TEN_MINUTES_MILLS = 10 * 60 * 1000;

    private static final String WIN_DATETIME_FORMAT = "yyyy-MM-dd_HH-mm-ss";

    private static final String DEFAULT_DATETIME_FORMAT = "yyyy-MM-dd_HH:mm:ss";

    private static Semaphore guard = new Semaphore(1);

    private static final String USER_HOME = System.getProperty("user.home");

    private final Set<ThreadPoolExhaustedListener> listeners = new ConcurrentHashSet<>();

    public AbortPolicyWithReport(String threadName, URL url) {
        this.threadName = threadName;
        this.url = url;
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        String msg = String.format("Thread pool is EXHAUSTED!" +
                        " Thread Name: %s, Pool Size: %d (active: %d, core: %d, max: %d, largest: %d)," +
                        " Task: %d (completed: %d)," +
                        " Executor status:(isShutdown:%s, isTerminated:%s, isTerminating:%s), in %s://%s:%d!",
                threadName, e.getPoolSize(), e.getActiveCount(), e.getCorePoolSize(), e.getMaximumPoolSize(),
                e.getLargestPoolSize(),
                e.getTaskCount(), e.getCompletedTaskCount(), e.isShutdown(), e.isTerminated(), e.isTerminating(),
                url.getProtocol(), url.getIp(), url.getPort());
        logger.warn(msg);
        dumpJStack();
        dispatchThreadPoolExhaustedEvent(msg);
        throw new RejectedExecutionException(msg);
    }

    public void addThreadPoolExhaustedEventListener(ThreadPoolExhaustedListener listener) {
        listeners.add(listener);
    }

    public void removeThreadPoolExhaustedEventListener(ThreadPoolExhaustedListener listener) {
        listeners.remove(listener);
    }

    /**
     * dispatch ThreadPoolExhaustedEvent
     * @param msg
     */
    public void dispatchThreadPoolExhaustedEvent(String msg) {
        listeners.forEach(listener -> listener.onEvent(new ThreadPoolExhaustedEvent(msg)));
    }

    private void dumpJStack() {
        long now = System.currentTimeMillis();

        //dump every 10 minutes
        if (now - lastPrintTime < TEN_MINUTES_MILLS) {
            return;
        }

        if (!guard.tryAcquire()) {
            return;
        }

        ExecutorService pool = Executors.newSingleThreadExecutor();
        pool.execute(() -> {
            String dumpPath = getDumpPath();

            SimpleDateFormat sdf;

            String os = System.getProperty(OS_NAME_KEY).toLowerCase();

            // window system don't support ":" in file name
            if (os.contains(OS_WIN_PREFIX)) {
                sdf = new SimpleDateFormat(WIN_DATETIME_FORMAT);
            } else {
                sdf = new SimpleDateFormat(DEFAULT_DATETIME_FORMAT);
            }

            String dateStr = sdf.format(new Date());
            //try-with-resources
            try (FileOutputStream jStackStream = new FileOutputStream(
                    new File(dumpPath, "Dubbo_JStack.log" + "." + dateStr))) {
                JVMUtil.jstack(jStackStream);
            } catch (Throwable t) {
                logger.error("dump jStack error", t);
            } finally {
                guard.release();
            }
            lastPrintTime = System.currentTimeMillis();
        });
        //must shutdown thread pool ,if not will lead to OOM
        pool.shutdown();

    }

    private String getDumpPath() {
        final String dumpPath = url.getParameter(DUMP_DIRECTORY);
        if (StringUtils.isEmpty(dumpPath)) {
            return USER_HOME;
        }
        final File dumpDirectory = new File(dumpPath);
        if (!dumpDirectory.exists()) {
            if (dumpDirectory.mkdirs()) {
                logger.info(format("Dubbo dump directory[%s] created", dumpDirectory.getAbsolutePath()));
            } else {
                logger.warn(format("Dubbo dump directory[%s] can't be created, use the 'user.home'[%s]",
                        dumpDirectory.getAbsolutePath(), USER_HOME));
                return USER_HOME;
            }
        }
        return dumpPath;
    }
}
