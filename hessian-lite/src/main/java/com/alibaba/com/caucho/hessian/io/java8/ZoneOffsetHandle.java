package com.alibaba.com.caucho.hessian.io.java8;

import com.alibaba.com.caucho.hessian.io.HessianHandle;

import java.io.Serializable;
import java.lang.reflect.Method;

@SuppressWarnings("unchecked")
public class ZoneOffsetHandle implements HessianHandle, Serializable {
    private static final long serialVersionUID = 8841589723587858789L;

    private int seconds;

    public ZoneOffsetHandle() {
    }

    public ZoneOffsetHandle(Object o) {
        try {
            Class c = Class.forName("java.time.ZoneOffset");
            Method m = c.getDeclaredMethod("getTotalSeconds");
            this.seconds = (Integer) m.invoke(o);
        } catch (Throwable t) {
            // ignore
        }
    }

    private Object readResolve() {
        try {
            Class c = Class.forName("java.time.ZoneOffset");
            Method m = c.getDeclaredMethod("ofTotalSeconds", int.class);
            return m.invoke(null, seconds);
        } catch (Throwable t) {
            // ignore
        }
        return null;
    }
}
