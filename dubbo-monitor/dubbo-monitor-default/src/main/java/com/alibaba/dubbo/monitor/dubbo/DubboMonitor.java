/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.monitor.dubbo;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.monitor.Monitor;
import com.alibaba.dubbo.monitor.MonitorService;
import com.alibaba.dubbo.rpc.Invoker;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * DubboMonitor
 *
 * @author william.liangf
 */
public class DubboMonitor implements Monitor {

    private static final Logger logger = LoggerFactory.getLogger(DubboMonitor.class);

    private static final int LENGTH = 10;

    // 定时任务执行器
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(3, new NamedThreadFactory("DubboMonitorSendTimer", true));

    // 统计信息收集定时器
    private final ScheduledFuture<?> sendFuture;

    private final Invoker<MonitorService> monitorInvoker;

    private final MonitorService monitorService;

    private final long monitorInterval;

    private final ConcurrentMap<Statistics, AtomicReference<long[]>> statisticsMap = new ConcurrentHashMap<Statistics, AtomicReference<long[]>>();

    public DubboMonitor(Invoker<MonitorService> monitorInvoker, MonitorService monitorService) {
        this.monitorInvoker = monitorInvoker;
        this.monitorService = monitorService;
        this.monitorInterval = monitorInvoker.getUrl().getPositiveParameter("interval", 60000);
        // 启动统计信息收集定时器
        sendFuture = scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                // 收集统计信息
                try {
                    send();
                } catch (Throwable t) { // 防御性容错
                    logger.error("Unexpected error occur at send statistic, cause: " + t.getMessage(), t);
                }
            }
        }, monitorInterval, monitorInterval, TimeUnit.MILLISECONDS);
    }

    public void send() {
        if (logger.isInfoEnabled()) {
            logger.info("Send statistics to monitor " + getUrl());
        }
        String timestamp = String.valueOf(System.currentTimeMillis());
        for (Map.Entry<Statistics, AtomicReference<long[]>> entry : statisticsMap.entrySet()) {
            // 获取已统计数据
            Statistics statistics = entry.getKey();
            AtomicReference<long[]> reference = entry.getValue();
            long[] numbers = reference.get();
            long success = numbers[0];
            long failure = numbers[1];
            long input = numbers[2];
            long output = numbers[3];
            long elapsed = numbers[4];
            long concurrent = numbers[5];
            long maxInput = numbers[6];
            long maxOutput = numbers[7];
            long maxElapsed = numbers[8];
            long maxConcurrent = numbers[9];

            // 发送汇总信息
            URL url = statistics.getUrl()
                    .addParameters(MonitorService.TIMESTAMP, timestamp,
                            MonitorService.SUCCESS, String.valueOf(success),
                            MonitorService.FAILURE, String.valueOf(failure),
                            MonitorService.INPUT, String.valueOf(input),
                            MonitorService.OUTPUT, String.valueOf(output),
                            MonitorService.ELAPSED, String.valueOf(elapsed),
                            MonitorService.CONCURRENT, String.valueOf(concurrent),
                            MonitorService.MAX_INPUT, String.valueOf(maxInput),
                            MonitorService.MAX_OUTPUT, String.valueOf(maxOutput),
                            MonitorService.MAX_ELAPSED, String.valueOf(maxElapsed),
                            MonitorService.MAX_CONCURRENT, String.valueOf(maxConcurrent)
                    );
            monitorService.collect(url);

            // 减掉已统计数据
            long[] current;
            long[] update = new long[LENGTH];
            do {
                current = reference.get();
                if (current == null) {
                    update[0] = 0;
                    update[1] = 0;
                    update[2] = 0;
                    update[3] = 0;
                    update[4] = 0;
                    update[5] = 0;
                } else {
                    update[0] = current[0] - success;
                    update[1] = current[1] - failure;
                    update[2] = current[2] - input;
                    update[3] = current[3] - output;
                    update[4] = current[4] - elapsed;
                    update[5] = current[5] - concurrent;
                }
            } while (!reference.compareAndSet(current, update));
        }
    }

    public void collect(URL url) {
        // 读写统计变量
        int success = url.getParameter(MonitorService.SUCCESS, 0);
        int failure = url.getParameter(MonitorService.FAILURE, 0);
        int input = url.getParameter(MonitorService.INPUT, 0);
        int output = url.getParameter(MonitorService.OUTPUT, 0);
        int elapsed = url.getParameter(MonitorService.ELAPSED, 0);
        int concurrent = url.getParameter(MonitorService.CONCURRENT, 0);
        // 初始化原子引用
        Statistics statistics = new Statistics(url);
        AtomicReference<long[]> reference = statisticsMap.get(statistics);
        if (reference == null) {
            statisticsMap.putIfAbsent(statistics, new AtomicReference<long[]>());
            reference = statisticsMap.get(statistics);
        }
        // CompareAndSet并发加入统计数据
        long[] current;
        long[] update = new long[LENGTH];
        do {
            current = reference.get();
            if (current == null) {
                update[0] = success;
                update[1] = failure;
                update[2] = input;
                update[3] = output;
                update[4] = elapsed;
                update[5] = concurrent;
                update[6] = input;
                update[7] = output;
                update[8] = elapsed;
                update[9] = concurrent;
            } else {
                update[0] = current[0] + success;
                update[1] = current[1] + failure;
                update[2] = current[2] + input;
                update[3] = current[3] + output;
                update[4] = current[4] + elapsed;
                update[5] = (current[5] + concurrent) / 2;
                update[6] = current[6] > input ? current[6] : input;
                update[7] = current[7] > output ? current[7] : output;
                update[8] = current[8] > elapsed ? current[8] : elapsed;
                update[9] = current[9] > concurrent ? current[9] : concurrent;
            }
        } while (!reference.compareAndSet(current, update));
    }

    public List<URL> lookup(URL query) {
        return monitorService.lookup(query);
    }

    public URL getUrl() {
        return monitorInvoker.getUrl();
    }

    public boolean isAvailable() {
        return monitorInvoker.isAvailable();
    }

    public void destroy() {
        try {
            sendFuture.cancel(true);
        } catch (Throwable t) {
            logger.error("Unexpected error occur at cancel sender timer, cause: " + t.getMessage(), t);
        }
        monitorInvoker.destroy();
    }

}