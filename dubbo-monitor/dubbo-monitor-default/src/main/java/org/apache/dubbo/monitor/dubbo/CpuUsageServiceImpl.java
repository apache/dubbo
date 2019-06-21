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
    private CpuUsageGaugeSet cpuUsage;
    private static final String PATH = "/proc/stat";

    public CpuUsageServiceImpl(Long dataTimeToLive, Long collectCpuUsageInMill) {
        cpuUsage = new CpuUsageGaugeSet(dataTimeToLive, TimeUnit.MILLISECONDS, PATH, new ManualClock());
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory(this.getClass().getSimpleName(), true));
        executor.scheduleAtFixedRate(this::collectCpuUsage, collectCpuUsageInMill, collectCpuUsageInMill, TimeUnit.MILLISECONDS);
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
        listeners.forEach((key, value) -> value.cpuChanged(user));
    }
}
