package com.alibaba.com.caucho.hessian.io.java8;

import com.alibaba.com.caucho.hessian.io.HessianHandle;

import java.io.Serializable;
import java.lang.reflect.Method;

@SuppressWarnings("unchecked")
public class YearHandle implements HessianHandle, Serializable {
    private static final long serialVersionUID = -6299552890287487926L;

    private int year;

    public YearHandle() {
    }

    public YearHandle(Object o) {
        try {
            Class c = Class.forName("java.time.Year");
            Method m = c.getDeclaredMethod("getValue");
            this.year = (Integer) m.invoke(o);
        } catch (Throwable t) {
            // ignore
        }

    }

    private Object readResolve() {
        try {
            Class c = Class.forName("java.time.Year");
            Method m = c.getDeclaredMethod("of", int.class);
            return m.invoke(null, year);
        } catch (Throwable t) {
            // ignore
        }
        return null;
    }
}
