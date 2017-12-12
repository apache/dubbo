package com.alibaba.com.caucho.hessian.io.java8;

import com.alibaba.com.caucho.hessian.io.HessianHandle;

import java.io.Serializable;
import java.lang.reflect.Method;

@SuppressWarnings("unchecked")
public class OffsetDateTimeHandle implements HessianHandle, Serializable {
    private static final long serialVersionUID = -7823900532640515312L;

    private Object dateTime;
    private Object offset;

    public OffsetDateTimeHandle() {
    }

    public OffsetDateTimeHandle(Object o) {
        try {
            Class c = Class.forName("java.time.OffsetDateTime");
            Method m = c.getDeclaredMethod("toLocalDateTime");
            this.dateTime = m.invoke(o);
            m = c.getDeclaredMethod("getOffset");
            this.offset = m.invoke(o);
        } catch (Throwable t) {
            // ignore
        }
    }

    private Object readResolve() {

        try {
            Class c = Class.forName("java.time.OffsetDateTime");
            Method m = c.getDeclaredMethod("of", Class.forName("java.time.LocalDateTime"),
                    Class.forName("java.time.ZoneOffset"));
            return m.invoke(null, dateTime, offset);
        } catch (Throwable t) {
            // ignore
        }
        return null;
    }
}
