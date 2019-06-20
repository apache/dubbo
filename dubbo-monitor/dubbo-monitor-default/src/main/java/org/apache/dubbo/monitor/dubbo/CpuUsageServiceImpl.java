package org.apache.dubbo.monitor.dubbo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CpuUsageServiceImpl implements CpuUsageService {
    private final Map<String, CpuUsageListener> listeners = new ConcurrentHashMap<>();

    @Override
    public void addListener(String key, CpuUsageListener cpuListener) {
        listeners.put(key, cpuListener);
    }

    @Override
    public void removeListener(String key, CpuUsageListener cpuListener) {
       listeners.remove(key, cpuListener);
    }
}
