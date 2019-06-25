package org.apache.dubbo.rpc.cluster.loadbalance.statistics;

public interface CpuUsageService {
    void addListener(String key, CpuUsageListener cpuListener);

    void removeListener(String key, CpuUsageListener cpuListener);
}
