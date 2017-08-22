package com.alibaba.com.caucho.hessian.io.java8;


import java.io.Serializable;
import java.time.LocalTime;

import com.alibaba.com.caucho.hessian.io.HessianHandle;

public class LocalTimeHandle implements HessianHandle, Serializable {
    private static final long serialVersionUID = -5892919085390462315L;

    /**
     * The hour.
     */
    private int hour;
    /**
     * The minute.
     */
    private int minute;
    /**
     * The second.
     */
    private int second;
    /**
     * The nanosecond.
     */
    private int nano;

    public LocalTimeHandle() {
    }

    public LocalTimeHandle(LocalTime localTime) {
        this.hour = localTime.getHour();
        this.minute = localTime.getMinute();
        this.second = localTime.getSecond();
        this.nano = localTime.getNano();
    }

    private Object readResolve() {
        return LocalTime.of(hour, minute, second, nano);
    }
}
