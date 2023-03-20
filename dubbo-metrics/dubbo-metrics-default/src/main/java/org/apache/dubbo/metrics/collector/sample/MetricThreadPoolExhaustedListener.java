package org.apache.dubbo.metrics.collector.sample;

import org.apache.dubbo.common.threadpool.event.ThreadPoolExhaustedEvent;
import org.apache.dubbo.common.threadpool.event.ThreadPoolExhaustedListener;
import org.apache.dubbo.metrics.collector.DefaultMetricsCollector;

public class MetricThreadPoolExhaustedListener implements ThreadPoolExhaustedListener {

    private final ThreadRejectMetricsCountSampler threadRejectMetricsCountSampler;

    private final String threadPoolExecutorName;

    public MetricThreadPoolExhaustedListener(String threadPoolExecutorName,DefaultMetricsCollector collector) {
        this.threadPoolExecutorName=threadPoolExecutorName;
        this.threadRejectMetricsCountSampler = new ThreadRejectMetricsCountSampler(collector);
    }

    public MetricThreadPoolExhaustedListener(String threadPoolExecutorName,ThreadRejectMetricsCountSampler sampler) {
        this.threadPoolExecutorName=threadPoolExecutorName;
        this.threadRejectMetricsCountSampler=sampler;
    }
    @Override
    public void onEvent(ThreadPoolExhaustedEvent event) {
        threadRejectMetricsCountSampler.addMetricName(threadPoolExecutorName);
        threadRejectMetricsCountSampler.incOnEvent(threadPoolExecutorName,threadPoolExecutorName);
    }
}
