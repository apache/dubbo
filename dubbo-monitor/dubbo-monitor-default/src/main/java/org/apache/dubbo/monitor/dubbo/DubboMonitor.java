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
package org.apache.dubbo.monitor.dubbo;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ExecutorUtil;
import org.apache.dubbo.monitor.Monitor;
import org.apache.dubbo.monitor.MonitorService;
import org.apache.dubbo.rpc.Invoker;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_PROTOCOL;
import static org.apache.dubbo.common.constants.CommonConstants.TIMESTAMP_KEY;
import static org.apache.dubbo.monitor.Constants.CONCURRENT_KEY;
import static org.apache.dubbo.monitor.Constants.DEFAULT_MONITOR_SEND_DATA_INTERVAL;
import static org.apache.dubbo.monitor.Constants.ELAPSED_KEY;
import static org.apache.dubbo.monitor.Constants.FAILURE_KEY;
import static org.apache.dubbo.monitor.Constants.INPUT_KEY;
import static org.apache.dubbo.monitor.Constants.MAX_CONCURRENT_KEY;
import static org.apache.dubbo.monitor.Constants.MAX_ELAPSED_KEY;
import static org.apache.dubbo.monitor.Constants.MAX_INPUT_KEY;
import static org.apache.dubbo.monitor.Constants.MAX_OUTPUT_KEY;
import static org.apache.dubbo.monitor.Constants.MONITOR_SEND_DATA_INTERVAL_KEY;
import static org.apache.dubbo.monitor.Constants.OUTPUT_KEY;
import static org.apache.dubbo.monitor.Constants.SUCCESS_KEY;

/**
 * DubboMonitor
 */
public class DubboMonitor implements Monitor {

    private static final Logger logger = LoggerFactory.getLogger(DubboMonitor.class);

    /**
     * The timer for sending statistics
     */
    private final ScheduledExecutorService scheduledExecutorService;

    /**
     * The future that can cancel the <b>scheduledExecutorService</b>
     */
    private final ScheduledFuture<?> sendFuture;

    private final Invoker<MonitorService> monitorInvoker;

    private final MonitorService monitorService;

    private final ConcurrentMap<Statistics, AtomicReference<StatisticsItem>> statisticsMap = new ConcurrentHashMap<>();

    public DubboMonitor(Invoker<MonitorService> monitorInvoker, MonitorService monitorService) {
        this.monitorInvoker = monitorInvoker;
        this.monitorService = monitorService;
        scheduledExecutorService = monitorInvoker.getUrl().getOrDefaultApplicationModel().getApplicationExecutorRepository().getSharedScheduledExecutor();
        // The time interval for timer <b>scheduledExecutorService</b> to send data
        final long monitorInterval = monitorInvoker.getUrl().getPositiveParameter(MONITOR_SEND_DATA_INTERVAL_KEY, DEFAULT_MONITOR_SEND_DATA_INTERVAL);
        // collect timer for collecting statistics data
        sendFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                // collect data
                send();
            } catch (Throwable t) {
                logger.error("Unexpected error occur at send statistic, cause: " + t.getMessage(), t);
            }
        }, monitorInterval, monitorInterval, TimeUnit.MILLISECONDS);
    }

    public void send() {
        if (logger.isDebugEnabled()) {
            logger.debug("Send statistics to monitor " + getUrl());
        }

        String timestamp = String.valueOf(System.currentTimeMillis());
        for (Map.Entry<Statistics, AtomicReference<StatisticsItem>> entry : statisticsMap.entrySet()) {
            // get statistics data
            Statistics statistics = entry.getKey();
            AtomicReference<StatisticsItem> reference = entry.getValue();
            StatisticsItem statisticsItem = reference.get();

            // send statistics data
            URL url = statistics.getUrl()
                .addParameters(TIMESTAMP_KEY, timestamp,
                    SUCCESS_KEY, String.valueOf(statisticsItem.getSuccess()),
                    FAILURE_KEY, String.valueOf(statisticsItem.getFailure()),
                    INPUT_KEY, String.valueOf(statisticsItem.getInput()),
                    OUTPUT_KEY, String.valueOf(statisticsItem.getOutput()),
                    ELAPSED_KEY, String.valueOf(statisticsItem.getElapsed()),
                    CONCURRENT_KEY, String.valueOf(statisticsItem.getConcurrent()),
                    MAX_INPUT_KEY, String.valueOf(statisticsItem.getMaxInput()),
                    MAX_OUTPUT_KEY, String.valueOf(statisticsItem.getMaxOutput()),
                    MAX_ELAPSED_KEY, String.valueOf(statisticsItem.getMaxElapsed()),
                    MAX_CONCURRENT_KEY, String.valueOf(statisticsItem.getMaxConcurrent()),
                    DEFAULT_PROTOCOL, getUrl().getParameter(DEFAULT_PROTOCOL)
                );
            monitorService.collect(url.toSerializableURL());

            // reset
            StatisticsItem current;
            StatisticsItem update = new StatisticsItem();
            do {
                current = reference.get();
                if (current == null) {
                    update.setItems(0, 0, 0, 0, 0, 0);
                } else {
                    update.setItems(
                        current.getSuccess() - statisticsItem.getSuccess(),
                        current.getFailure() - statisticsItem.getFailure(),
                        current.getInput() - statisticsItem.getInput(),
                        current.getOutput() - statisticsItem.getOutput(),
                        current.getElapsed() - statisticsItem.getElapsed(),
                        current.getConcurrent() - statisticsItem.getConcurrent()
                    );
                }
            } while (!reference.compareAndSet(current, update));
        }
    }

    @Override
    public void collect(URL url) {
        // data to collect from url
        int success = url.getParameter(SUCCESS_KEY, 0);
        int failure = url.getParameter(FAILURE_KEY, 0);
        int input = url.getParameter(INPUT_KEY, 0);
        int output = url.getParameter(OUTPUT_KEY, 0);
        int elapsed = url.getParameter(ELAPSED_KEY, 0);
        int concurrent = url.getParameter(CONCURRENT_KEY, 0);
        // init atomic reference
        Statistics statistics = new Statistics(url);
        AtomicReference<StatisticsItem> reference = statisticsMap.computeIfAbsent(statistics, k -> new AtomicReference<>());
        // use CompareAndSet to sum
        StatisticsItem current;
        StatisticsItem update = new StatisticsItem();
        do {
            current = reference.get();
            if (current == null) {
                update.setItems(success, failure, input, output, elapsed, concurrent, input, output, elapsed, concurrent);
            } else {
                update.setItems(
                    current.getSuccess() + success,
                    current.getFailure() + failure,
                    current.getInput() + input,
                    current.getOutput() + output,
                    current.getElapsed() + elapsed,
                    (current.getConcurrent() + concurrent) / 2,
                    current.getMaxInput() > input ? current.getMaxInput() : input,
                    current.getMaxOutput() > output ? current.getMaxOutput() : output,
                    current.getMaxElapsed() > elapsed ? current.getMaxElapsed() : elapsed,
                    current.getMaxConcurrent() > concurrent ? current.getMaxConcurrent() : concurrent
                );
            }
        } while (!reference.compareAndSet(current, update));
    }

    @Override
    public List<URL> lookup(URL query) {
        return monitorService.lookup(query);
    }

    @Override
    public URL getUrl() {
        return monitorInvoker.getUrl();
    }

    @Override
    public boolean isAvailable() {
        return monitorInvoker.isAvailable();
    }

    @Override
    public void destroy() {
        try {
            ExecutorUtil.cancelScheduledFuture(sendFuture);
        } catch (Throwable t) {
            logger.error("Unexpected error occur at cancel sender timer, cause: " + t.getMessage(), t);
        }
        monitorInvoker.destroy();
    }

}
