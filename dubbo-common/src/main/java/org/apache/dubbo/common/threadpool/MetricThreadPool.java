package org.apache.dubbo.common.threadpool;

import org.apache.dubbo.common.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.common.metrics.model.ThreadPoolMetric;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.concurrent.ThreadPoolExecutor;

public abstract class MetricThreadPool implements ThreadPool {

    private DefaultMetricsCollector collector = null;

    private ApplicationModel applicationModel;

    @Override
    public void setApplicationModel(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
        collector = applicationModel.getBeanFactory().getBean(DefaultMetricsCollector.class);
    }

    protected void addThreadPoolMetric(ThreadPoolExecutor threadPoolExecutor, String threadPoolName) {
        ThreadPoolMetric metric = new ThreadPoolMetric(this.applicationModel.getApplicationName(), threadPoolName, threadPoolExecutor);
        collector.addThreadPoolMetric(metric);
    }
}
