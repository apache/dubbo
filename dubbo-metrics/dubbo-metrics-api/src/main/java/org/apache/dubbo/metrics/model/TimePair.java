package org.apache.dubbo.metrics.model;

public class TimePair {

    private final long begin;
    private long end;

    public TimePair(long currentTimeMillis) {
        this.begin = currentTimeMillis;
    }

    public static TimePair start() {
        return new TimePair(System.currentTimeMillis());
    }

    public void end() {
        this.end = System.currentTimeMillis();
    }

    public long calc() {
        return end - begin;
    }
}
