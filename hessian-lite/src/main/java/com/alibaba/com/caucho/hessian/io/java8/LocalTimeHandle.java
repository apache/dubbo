package com.alibaba.com.caucho.hessian.io.java8;


import com.alibaba.com.caucho.hessian.io.HessianHandle;

import java.io.Serializable;
import java.lang.reflect.Method;

@SuppressWarnings("unchecked")
public class LocalTimeHandle implements HessianHandle, Serializable {
    private static final long serialVersionUID = -5892919085390462315L;

    private int hour;
    private int minute;
    private int second;
    private int nano;

    public LocalTimeHandle() {
    }

    public LocalTimeHandle(Object o) {
        try {
            Class c = Class.forName("java.time.LocalTime");
            Method m = c.getDeclaredMethod("getHour");
            this.hour = (Integer) m.invoke(o);
            m = c.getDeclaredMethod("getMinute");
            this.minute = (Integer) m.invoke(o);
            m = c.getDeclaredMethod("getSecond");
            this.second = (Integer) m.invoke(o);
            m = c.getDeclaredMethod("getNano");
            this.nano = (Integer) m.invoke(o);
        } catch (Throwable t) {
            // ignore
        }
    }

    private Object readResolve() {
        try {
            Class c = Class.forName("java.time.LocalTime");
            Method m = c.getDeclaredMethod("of", int.class, int.class, int.class, int.class);
            return m.invoke(null, hour, minute, second, nano);
        } catch (Throwable t) {
            // ignore
        }
        return null;
    }
}
