package org.apache.dubbo.monitor.dubbo;

import com.alibaba.metrics.CachedGauge;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A cached gauge for load balancer statistics
 */
public class CpuUsageGauge extends CachedGauge<AtomicLong> {
    private AtomicLong cpuValue;

    public CpuUsageGauge(long timeout, TimeUnit timeoutUnit) {
        super(timeout, timeoutUnit);
    }

    public CpuUsageGauge(long timeout, TimeUnit timeoutUnit, AtomicLong value) {
        super(timeout, timeoutUnit);
        this.cpuValue = value;
    }

    @Override
    protected AtomicLong loadValue() {
        return cpuValue;
    }

    public void setCpuValue(AtomicLong cpuValue) {
        this.cpuValue = cpuValue;
    }
}
