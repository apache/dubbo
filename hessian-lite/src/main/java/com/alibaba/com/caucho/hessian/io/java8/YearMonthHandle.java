package com.alibaba.com.caucho.hessian.io.java8;

import java.io.Serializable;
import java.time.YearMonth;

import com.alibaba.com.caucho.hessian.io.HessianHandle;

public class YearMonthHandle implements HessianHandle, Serializable {
	
    private static final long serialVersionUID = -4150786187896925314L;

    /**
     * The year.
     */
    private int year;
    /**
     * The month-of-year, not null.
     */
    private int month;


    public YearMonthHandle() {
    }

    public YearMonthHandle(YearMonth yearMonth) {
        this.year = yearMonth.getYear();
        this.month = yearMonth.getMonthValue();
    }

    private Object readResolve() {
        return YearMonth.of(year, month);
    }
}
