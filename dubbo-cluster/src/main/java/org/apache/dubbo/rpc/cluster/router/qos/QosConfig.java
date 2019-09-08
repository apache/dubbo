package org.apache.dubbo.rpc.cluster.router.qos;

public class QosConfig {

    private volatile boolean isQosEnabled = true;

    private volatile int timeWindowInSeconds = 10;
    private volatile int requestThreshold = 10;
    private volatile double errorRateThreshold = 0.5;
    private volatile double maxIsolationRate = 1;
    // 如果是0，就默认关闭
    private volatile long isolationTime = 60 * 1000;
    private volatile long maxIsolationTimeMultiple = 60;

    public boolean isQosEnabled() {
        return isQosEnabled;
    }

    public int getTimeWindowInSeconds() {
        return timeWindowInSeconds;
    }

    public int getRequestThreshold() {
        return requestThreshold;
    }

    public double getErrorRateThreshold() {
        return errorRateThreshold;
    }

    public double getMaxIsolationRate() {
        return maxIsolationRate;
    }

    public long getIsolationTime() {
        return isolationTime;
    }

    public long getMaxIsolationTimeMultiple() {
        return maxIsolationTimeMultiple;
    }
}
