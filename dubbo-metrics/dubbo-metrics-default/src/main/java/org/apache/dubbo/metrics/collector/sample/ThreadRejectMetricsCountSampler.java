package org.apache.dubbo.metrics.collector.sample;

import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.metrics.model.*;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.ToDoubleFunction;
import static org.apache.dubbo.metrics.model.MetricsCategory.THREAD_POOL;

public class ThreadRejectMetricsCountSampler extends SimpleMetricsCountSampler<String, String, ThreadPoolRejectMetric> {

    private final DefaultMetricsCollector collector;

    private final Set<String> metricNames = new ConcurrentHashSet<>();
    public ThreadRejectMetricsCountSampler(DefaultMetricsCollector collector) {
        this.collector = collector;
        this.collector.addSampler(this);
    }

    public void addMetricName(String name){
        this.metricNames.add(name);
    }

    @Override
    public List<MetricSample> sample() {
        List<MetricSample> metricSamples = new ArrayList<>();
        metricNames.stream().forEach(name->collect(metricSamples,name));
        return metricSamples;
    }


    private void collect(List<MetricSample> list, String metricName) {
        count(list, metricName, MetricsKey.THREAD_POOL_THREAD_REJECT_COUNT);
    }

    private <T extends Metric> void count(List<MetricSample> list, String metricName, MetricsKey metricsKey) {
        getCount(metricName).filter(e -> !e.isEmpty())
            .ifPresent(map -> map.forEach((k, v) ->
                list.add(getGaugeMetricSample(metricsKey, k, THREAD_POOL, v, AtomicLong::get))));
    }

    private <T> GaugeMetricSample<T> getGaugeMetricSample(MetricsKey metricsKey,
                                                          ThreadPoolRejectMetric methodMetric,
                                                          MetricsCategory metricsCategory,
                                                          T value,
                                                          ToDoubleFunction<T> apply) {
        return new GaugeMetricSample<>(
            metricsKey.getNameByType(methodMetric.getThreadPoolName()),
            metricsKey.getDescription(),
            methodMetric.getTags(),
            metricsCategory,
            value,
            apply);
    }

    @Override
    protected void countConfigure(MetricsCountSampleConfigurer<String, String, ThreadPoolRejectMetric> sampleConfigure) {
        sampleConfigure.configureMetrics(configure -> new ThreadPoolRejectMetric(collector.getApplicationName(),configure.getSource()));
    }
}
