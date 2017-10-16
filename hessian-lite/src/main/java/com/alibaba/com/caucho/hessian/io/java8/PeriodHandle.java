package com.alibaba.com.caucho.hessian.io.java8;

import com.alibaba.com.caucho.hessian.io.HessianHandle;

import java.io.Serializable;
import java.lang.reflect.Method;


@SuppressWarnings("unchecked")
public class PeriodHandle implements HessianHandle, Serializable {
    private static final long serialVersionUID = 4399720381283781186L;

    private int years;
    private int months;
    private int days;

    public PeriodHandle() {
    }

    public PeriodHandle(Object o) {
        try {
            Class c = Class.forName("java.time.Period");
            Method m = c.getDeclaredMethod("getYears");
            this.years = (Integer) m.invoke(o);
            m = c.getDeclaredMethod("getMonths");
            this.months = (Integer) m.invoke(o);
            m = c.getDeclaredMethod("getDays");
            this.days = (Integer) m.invoke(o);
        } catch (Throwable t) {
            // ignore
        }
    }

    private Object readResolve() {
        try {
            Class c = Class.forName("java.time.Period");
            Method m = c.getDeclaredMethod("of", int.class, int.class, int.class);
            return m.invoke(null, years, months, days);
        } catch (Throwable t) {
            // ignore
        }
        return null;
    }
}
