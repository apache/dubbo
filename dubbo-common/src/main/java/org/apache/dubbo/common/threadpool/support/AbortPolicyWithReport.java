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

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.JVMUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Abort Policy.
 * Log warn info when abort.
 * 拒绝策略实现类。打印 JStack ，分析线程状态
 */
public class AbortPolicyWithReport extends ThreadPoolExecutor.AbortPolicy {

    protected static final Logger logger = LoggerFactory.getLogger(AbortPolicyWithReport.class);

    /**
     * 线程名
     */
    private final String threadName;
    /**
     * URL 对象
     */
    private final URL url;
    /**
     * 最后打印时间
     */
    private static volatile long lastPrintTime = 0;
    /**
     * 信号量，大小为 1 。
     */
    private static Semaphore guard = new Semaphore(1);

    public AbortPolicyWithReport(String threadName, URL url) {
        this.threadName = threadName;
        this.url = url;
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        // 打印告警日志
        String msg = String.format("Thread pool is EXHAUSTED!" +
                        " Thread Name: %s, Pool Size: %d (active: %d, core: %d, max: %d, largest: %d), Task: %d (completed: %d)," +
                        " Executor status:(isShutdown:%s, isTerminated:%s, isTerminating:%s), in %s://%s:%d!",
                threadName, e.getPoolSize(), e.getActiveCount(), e.getCorePoolSize(), e.getMaximumPoolSize(), e.getLargestPoolSize(),
                e.getTaskCount(), e.getCompletedTaskCount(), e.isShutdown(), e.isTerminated(), e.isTerminating(),
                url.getProtocol(), url.getIp(), url.getPort());
        logger.warn(msg);
        // 打印 JStack ，分析线程状态。
        dumpJStack();
        // 抛出 RejectedExecutionException 异常
        throw new RejectedExecutionException(msg);
    }
    /**
     * 打印 JStack
     */
    private void dumpJStack() {
        long now = System.currentTimeMillis();

        //dump every 10 minutes 每十分钟打印一次
        if (now - lastPrintTime < 10 * 60 * 1000) {
            return;
        }
        //获取信号量
        if (!guard.tryAcquire()) {
            return;
        }

        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                // 获得路径
                String dumpPath = url.getParameter(Constants.DUMP_DIRECTORY, System.getProperty("user.home"));

                SimpleDateFormat sdf;
                // 获得系统
                String OS = System.getProperty("os.name").toLowerCase();

                // window system don't support ":" in file name windows系统不支持:在文件名中
                if(OS.contains("win")){
                    sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
                }else {
                    sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
                }

                String dateStr = sdf.format(new Date());
                FileOutputStream jstackStream = null;
                try {
                    // 获得输出流
                    jstackStream = new FileOutputStream(new File(dumpPath, "Dubbo_JStack.log" + "." + dateStr));
                    // 打印 JStack
                    JVMUtil.jstack(jstackStream);
                } catch (Throwable t) {
                    logger.error("dump jstack error", t);
                } finally {
                    // 释放信号量
                    guard.release();
                    // 释放输出流
                    if (jstackStream != null) {
                        try {
                            jstackStream.flush();
                            jstackStream.close();
                        } catch (IOException e) {
                        }
                    }
                }
                // 记录最后打印时间
                lastPrintTime = System.currentTimeMillis();
            }
        });

    }

}
