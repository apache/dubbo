package org.apache.dubbo.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.apache.dubbo.common.utils.NamedThreadFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author songxiaosheng
 */
public class DubboMetrics implements MeterBinder {

    protected static volatile Set<Gauge.Builder> gaugeBuilder = new HashSet<>();
    private ScheduledExecutorService collectorSyncJobExecutor;
    private static final int DEFAULT_SCHEDULE_INITIAL_DELAY = 5;
    private static final int DEFAULT_SCHEDULE_PERIOD = 30;

    @Override
    public void bindTo(MeterRegistry registry) {
        NamedThreadFactory threadFactory = new NamedThreadFactory("metrics-collector-sync-registry-job", true);
        collectorSyncJobExecutor = Executors.newScheduledThreadPool(1, threadFactory);
        collectorSyncJobExecutor.scheduleWithFixedDelay(() -> {
            gaugeBuilder.forEach(gauge ->  gauge.register(registry) );
        }, DEFAULT_SCHEDULE_INITIAL_DELAY, DEFAULT_SCHEDULE_PERIOD, TimeUnit.SECONDS);
    }

    public void destroy() {
        if (collectorSyncJobExecutor != null) {
            collectorSyncJobExecutor.shutdownNow();
        }
    }
}

