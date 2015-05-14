package com.alibaba.com.caucho.hessian.io.java8;

import java.io.Serializable;
import java.time.LocalDate;

import com.alibaba.com.caucho.hessian.io.HessianHandle;

public class LocalDateHandle implements HessianHandle, Serializable {
    private static final long serialVersionUID = 166018689500019951L;

    /**
     * The year.
     */
    private int year;
    /**
     * The month-of-year.
     */
    private short month;
    /**
     * The day-of-month.
     */
    private short day;

    public LocalDateHandle() {
    }

    public LocalDateHandle(LocalDate localDate) {
        this.year = localDate.getYear();
        this.month = (short) localDate.getMonthValue();
        this.day = (short) localDate.getDayOfMonth();
    }

    public Object readResolve() {
        return LocalDate.of(year, month, day);
    }
}
