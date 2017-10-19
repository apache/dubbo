package com.alibaba.com.caucho.hessian.io.java8;

import com.alibaba.com.caucho.hessian.io.HessianHandle;

import java.io.Serializable;
import java.lang.reflect.Method;

@SuppressWarnings("unchecked")
public class LocalDateHandle implements HessianHandle, Serializable {
    private static final long serialVersionUID = 166018689500019951L;

    private int year;
    private int month;
    private int day;

    public LocalDateHandle() {
    }

    public LocalDateHandle(Object o) {
        try {
            Class c = Class.forName("java.time.LocalDate");
            Method m = c.getDeclaredMethod("getYear");
            this.year = (Integer) m.invoke(o);
            m = c.getDeclaredMethod("getMonthValue");
            this.month = (Integer) m.invoke(o);
            m = c.getDeclaredMethod("getDayOfMonth");
            this.day = (Integer) m.invoke(o);
        } catch (Throwable t) {
            // ignore
        }
    }

    public Object readResolve() {
        try {
            Class c = Class.forName("java.time.LocalDate");
            Method m = c.getDeclaredMethod("of", int.class, int.class, int.class);
            return m.invoke(null, year, month, day);
        } catch (Throwable t) {
            // ignore
        }
        return null;
    }
}
