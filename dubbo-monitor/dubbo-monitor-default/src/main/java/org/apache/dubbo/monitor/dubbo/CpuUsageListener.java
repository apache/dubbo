package org.apache.dubbo.monitor.dubbo;

import com.alibaba.metrics.Gauge;

public interface CpuUsageListener {
    void cpuChanged(Gauge<Float> cpu);
}
