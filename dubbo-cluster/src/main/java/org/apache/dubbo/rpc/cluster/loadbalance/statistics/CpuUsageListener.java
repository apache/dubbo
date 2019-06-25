package org.apache.dubbo.rpc.cluster.loadbalance.statistics;

public interface CpuUsageListener {
    void cpuChanged(String ip, Float cpu);
}
