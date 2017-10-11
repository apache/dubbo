package com.alibaba.com.caucho.hessian.io.java8;

import com.alibaba.com.caucho.hessian.io.HessianHandle;

import java.io.Serializable;
import java.lang.reflect.Method;

@SuppressWarnings("unchecked")
public class YearMonthHandle implements HessianHandle, Serializable {
    private static final long serialVersionUID = -4150786187896925314L;

    private int year;
    private int month;

    public YearMonthHandle() {
    }

    public YearMonthHandle(Object o) {
        try {
            Class c = Class.forName("java.time.YearMonth");
            Method m = c.getDeclaredMethod("getYear");
            this.year = (Integer) m.invoke(o);
            m = c.getDeclaredMethod("getMonthValue");
            this.month = (Integer) m.invoke(o);
        } catch (Throwable t) {
            // ignore
        }
    }

    private Object readResolve() {
        try {
            Class c = Class.forName("java.time.YearMonth");
            Method m = c.getDeclaredMethod("of", int.class, int.class);
            return m.invoke(null, year, month);
        } catch (Throwable t) {
            // ignore
        }
        return null;
    }
}
