package org.apache.dubbo.monitor.dubbo;

import org.apache.dubbo.common.utils.NamedThreadFactory;

import com.alibaba.metrics.Gauge;
import com.alibaba.metrics.ManualClock;
import com.alibaba.metrics.MetricName;
import com.alibaba.metrics.os.linux.CpuUsageGaugeSet;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CpuUsageServiceImpl implements CpuUsageService {
    private final Map<String, CpuUsageListener> listeners = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor;
    private static final int THIRTY_MINUTES_IN_MILL = 3 * 60 * 1000;
    CpuUsageGaugeSet cpuUsage;

    public CpuUsageServiceImpl(String path, Long dataTimeToLive) {
        cpuUsage = new CpuUsageGaugeSet(dataTimeToLive, TimeUnit.MINUTES, path, new ManualClock());
        this.executor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory(this.getClass().getSimpleName(), true));
        executor.scheduleAtFixedRate(this::collectCpuUsage, THIRTY_MINUTES_IN_MILL, THIRTY_MINUTES_IN_MILL, TimeUnit.MILLISECONDS);
    }

    @Override
    public void addListener(String key, CpuUsageListener cpuListener) {
        listeners.put(key, cpuListener);
    }

    @Override
    public void removeListener(String key, CpuUsageListener cpuListener) {
       listeners.remove(key, cpuListener);
    }

    private void collectCpuUsage() {
        Gauge<Float> user = (Gauge) cpuUsage.getMetrics().get(MetricName.build("cpu.user"));
        listeners.forEach((key, value) -> value.cpuChanged(user.getValue()));
    }
}
