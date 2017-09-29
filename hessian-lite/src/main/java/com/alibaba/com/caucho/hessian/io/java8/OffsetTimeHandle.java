package com.alibaba.com.caucho.hessian.io.java8;

import com.alibaba.com.caucho.hessian.io.HessianHandle;

import java.io.Serializable;
import java.lang.reflect.Method;

@SuppressWarnings("unchecked")
public class OffsetTimeHandle implements HessianHandle, Serializable {
    private static final long serialVersionUID = -3269846941421652860L;

    private Object localTime;
    private Object zoneOffset;

    public OffsetTimeHandle() {
    }

    public OffsetTimeHandle(Object o) {
        try {
            Class c = Class.forName("java.time.OffsetTime");
            Method m = c.getDeclaredMethod("getOffset");
            this.zoneOffset = m.invoke(o);
            m = c.getDeclaredMethod("toLocalTime");
            this.localTime = m.invoke(o);
        } catch (Throwable t) {
            // ignore
        }
    }

    private Object readResolve() {
        try {
            Class c = Class.forName("java.time.OffsetTime");
            Method m = c.getDeclaredMethod("of", Class.forName("java.time.LocalTime"),
                    Class.forName("java.time.ZoneOffset"));
            return m.invoke(null, localTime, zoneOffset);
        } catch (Throwable t) {
            // ignore
        }
        return null;
    }
}
