package org.apache.dubbo.rpc.cluster.loadbalance.statistics;

import com.alibaba.metrics.Gauge;

public interface CpuUsageListener {
    void cpuChanged(String ip, Float cpu);
}
