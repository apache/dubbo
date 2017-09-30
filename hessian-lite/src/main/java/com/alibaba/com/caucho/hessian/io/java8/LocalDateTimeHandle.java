package com.alibaba.com.caucho.hessian.io.java8;

import com.alibaba.com.caucho.hessian.io.HessianHandle;

import java.io.Serializable;
import java.lang.reflect.Method;

@SuppressWarnings("unchecked")
public class LocalDateTimeHandle implements HessianHandle, Serializable {
    private static final long serialVersionUID = 7563825215275989361L;

    private Object date;
    private Object time;

    public LocalDateTimeHandle() {
    }

    public LocalDateTimeHandle(Object o) {
        try {
            Class c = Class.forName("java.time.LocalDateTime");
            Method m = c.getDeclaredMethod("toLocalDate");
            date = m.invoke(o);
            m = c.getDeclaredMethod("toLocalTime");
            time = m.invoke(o);
        } catch (Throwable t) {
            // ignore
        }
    }

    private Object readResolve() {
        try {
            Class c = Class.forName("java.time.LocalDateTime");
            Method m = c.getDeclaredMethod("of", Class.forName("java.time.LocalDate"),
                    Class.forName("java.time.LocalTime"));
            return m.invoke(null, date, time);
        } catch (Throwable t) {
            // ignore
        }
        return null;
    }
}
