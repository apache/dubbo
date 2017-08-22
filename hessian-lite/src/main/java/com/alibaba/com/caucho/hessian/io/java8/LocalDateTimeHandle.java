package com.alibaba.com.caucho.hessian.io.java8;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.alibaba.com.caucho.hessian.io.HessianHandle;

public class LocalDateTimeHandle implements HessianHandle, Serializable {
    private static final long serialVersionUID = 7563825215275989361L;

    /**
     * The date part.
     */
    private LocalDate date;
    /**
     * The time part.
     */
    private LocalTime time;

    public LocalDateTimeHandle() {
    }

    public LocalDateTimeHandle(LocalDateTime localDateTime) {
        this.date = localDateTime.toLocalDate();
        this.time = localDateTime.toLocalTime();
    }

    private Object readResolve() {
        return LocalDateTime.of(date, time);
    }
}
