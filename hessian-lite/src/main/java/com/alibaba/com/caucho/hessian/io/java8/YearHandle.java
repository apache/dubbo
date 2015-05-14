package com.alibaba.com.caucho.hessian.io.java8;

import java.io.Serializable;
import java.time.Year;
import java.time.temporal.ChronoField;

import com.alibaba.com.caucho.hessian.io.HessianHandle;

public class YearHandle implements HessianHandle, Serializable {
    private static final long serialVersionUID = -6299552890287487926L;

    private int year;

    public YearHandle() {
    }

    public YearHandle(Year year) {
        this.year = year.getValue();
    }

    private Object readResolve() {
        ChronoField.YEAR.checkValidValue(year);
        return Year.of(year);
    }
}
