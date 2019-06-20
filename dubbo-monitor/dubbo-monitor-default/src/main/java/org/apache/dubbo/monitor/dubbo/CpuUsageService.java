package org.apache.dubbo.monitor.dubbo;

public interface CpuUsageService {
    void addListener(String key, CpuUsageListener cpuListener);

    void removeListener(String key, CpuUsageListener cpuListener);
}
