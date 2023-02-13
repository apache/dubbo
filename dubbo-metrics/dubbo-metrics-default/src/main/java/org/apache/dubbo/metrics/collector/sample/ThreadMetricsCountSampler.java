package org.apache.dubbo.metrics.collector.sample;

import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.metrics.model.MetricsKey;
import org.apache.dubbo.metrics.model.ThreadPoolMetric;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import static org.apache.dubbo.metrics.model.MetricsCategory.THREAD_POOL;

public class ThreadMetricsCountSampler implements MetricsSampler {

    private DefaultMetricsCollector collector;
    private FrameworkExecutorRepository frameworkExecutorRepository;
    private Set<ThreadPoolMetric> threadPoolMetricSet = new HashSet<>();

    public ThreadMetricsCountSampler(DefaultMetricsCollector collector) {
        this.collector = collector;
        try{
            this.frameworkExecutorRepository = collector.getApplicationModel().getBeanFactory().getBean(FrameworkExecutorRepository.class);
        }catch(Exception ex){}
    }

    @Override
    public List<MetricSample> sample() {
        collect();
        List<MetricSample> metricSamples = new ArrayList<>();
        threadPoolMetricSet.forEach(e -> metricSamples.add(new GaugeMetricSample(MetricsKey.THREAD_POOL_CORE_SIZE, e.getTags(), THREAD_POOL, e::getCorePoolSize)));
        threadPoolMetricSet.forEach(e -> metricSamples.add(new GaugeMetricSample(MetricsKey.THREAD_POOL_LARGEST_SIZE, e.getTags(), THREAD_POOL, e::getLargestPoolSize)));
        threadPoolMetricSet.forEach(e -> metricSamples.add(new GaugeMetricSample(MetricsKey.THREAD_POOL_MAX_SIZE, e.getTags(), THREAD_POOL, e::getMaximumPoolSize)));
        threadPoolMetricSet.forEach(e -> metricSamples.add(new GaugeMetricSample(MetricsKey.THREAD_POOL_ACTIVE_SIZE, e.getTags(), THREAD_POOL, e::getActiveCount)));
        threadPoolMetricSet.forEach(e -> metricSamples.add(new GaugeMetricSample(MetricsKey.THREAD_POOL_THREAD_COUNT, e.getTags(), THREAD_POOL, e::getPoolSize)));
        threadPoolMetricSet.forEach(e -> metricSamples.add(new GaugeMetricSample(MetricsKey.THREAD_POOL_QUEUE_SIZE, e.getTags(), THREAD_POOL, e::getQueueSize)));

        return metricSamples;
    }

    private void collect() {
        if (frameworkExecutorRepository != null) {
            addThread("SharedExecutor", frameworkExecutorRepository.getSharedExecutor());
            addThread("MappingRefreshingExecutor", frameworkExecutorRepository.getMappingRefreshingExecutor());
            addThread("PoolRouterExecutor", frameworkExecutorRepository.getPoolRouterExecutor());
        }
    }

    private void addThread(String threadPoolName, ExecutorService executorService) {
        Optional<ExecutorService> executorOptional = Optional.ofNullable(executorService);
        if (executorOptional.isPresent() && executorOptional.get() instanceof ThreadPoolExecutor) {
            threadPoolMetricSet.add(
                    new ThreadPoolMetric(collector.getApplicationName(), threadPoolName,
                            (ThreadPoolExecutor) executorOptional.get()));
        }

    }
}
