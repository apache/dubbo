package com.alibaba.com.caucho.hessian.io.java8;

import java.io.Serializable;
import java.time.MonthDay;

import com.alibaba.com.caucho.hessian.io.HessianHandle;

public class MonthDayHandle implements HessianHandle, Serializable {

	private static final long serialVersionUID = 5288238558666577745L;
	
	/**
     * The month-of-year, not null.
     */
    private int month;
    /**
     * The day-of-month.
     */
    private int day;


    public MonthDayHandle() {
    }

    public MonthDayHandle(MonthDay monthDay) {
        this.month = monthDay.getMonthValue();
        this.day = monthDay.getDayOfMonth();
    }

    private Object readResolve() {
        return MonthDay.of(month, day);
    }
}
