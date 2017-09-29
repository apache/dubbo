package com.alibaba.com.caucho.hessian.io.java8;

import com.alibaba.com.caucho.hessian.io.HessianHandle;

import java.io.Serializable;
import java.lang.reflect.Method;


@SuppressWarnings("unchecked")
public class DurationHandle implements HessianHandle, Serializable {
    private static final long serialVersionUID = -4367309317780077156L;

    private long seconds;
    private int nanos;

    public DurationHandle() {
    }

    public DurationHandle(Object o) {
        try {
            Class c = Class.forName("java.time.Duration");
            Method m = c.getDeclaredMethod("getSeconds");
            this.seconds = (Long) m.invoke(o);
            m = c.getDeclaredMethod("getNano");
            this.nanos = (Integer) m.invoke(o);
        } catch (Throwable t) {
            // ignore
        }
    }

    private Object readResolve() {
        try {
            Class c = Class.forName("java.time.Duration");
            Method m = c.getDeclaredMethod("ofSeconds", long.class, long.class);
            return m.invoke(null, seconds, nanos);
        } catch (Throwable t) {
            // ignore
        }
        return null;
    }
}
