package org.apache.dubbo.monitor.dubbo;

import java.util.concurrent.atomic.AtomicLong;

public interface CpuUsageListener {
    void cpuChanged(AtomicLong cpu);
}
