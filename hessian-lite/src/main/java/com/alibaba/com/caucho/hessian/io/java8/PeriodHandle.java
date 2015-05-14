package com.alibaba.com.caucho.hessian.io.java8;

import java.io.Serializable;
import java.time.Period;

import com.alibaba.com.caucho.hessian.io.HessianHandle;

public class PeriodHandle implements HessianHandle, Serializable {
    private static final long serialVersionUID = 4399720381283781186L;
    /**
     * The number of years.
     */
    private int years;
    /**
     * The number of months.
     */
    private int months;
    /**
     * The number of days.
     */
    private int days;


    public PeriodHandle() {
    }

    public PeriodHandle(Period period) {
        this.years = period.getYears();
        this.months = period.getMonths();
        this.days = period.getDays();
    }

    private Object readResolve() {
        return Period.of(years, months, days);
    }
}
