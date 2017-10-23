package com.alibaba.com.caucho.hessian.io.java8;

import com.alibaba.com.caucho.hessian.io.HessianHandle;

import java.io.Serializable;
import java.lang.reflect.Method;

@SuppressWarnings("unchecked")
public class MonthDayHandle implements HessianHandle, Serializable {
    private static final long serialVersionUID = 5288238558666577745L;

    private int month;
    private int day;

    public MonthDayHandle() {
    }

    public MonthDayHandle(Object o) {
        try {
            Class c = Class.forName("java.time.MonthDay");
            Method m = c.getDeclaredMethod("getMonthValue");
            this.month = (Integer) m.invoke(o);
            m = c.getDeclaredMethod("getDayOfMonth");
            this.day = (Integer) m.invoke(o);
        } catch (Throwable t) {
            // ignore
        }
    }

    private Object readResolve() {
        try {
            Class c = Class.forName("java.time.MonthDay");
            Method m = c.getDeclaredMethod("of", int.class, int.class);
            return m.invoke(null, month, day);
        } catch (Throwable t) {
            // ignore
        }
        return null;
    }
}
