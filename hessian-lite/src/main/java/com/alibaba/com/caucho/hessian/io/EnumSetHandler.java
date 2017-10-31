package com.alibaba.com.caucho.hessian.io;

import java.io.Serializable;
import java.util.Arrays;
import java.util.EnumSet;

/**
 * @author bw on 24/10/2017.
 */
class EnumSetHandler implements Serializable, HessianHandle {
    private Class type;
    private Object[] objects;

    EnumSetHandler(Class type, Object[] objects) {
        this.type = type;
        this.objects = objects;
    }

    @SuppressWarnings("unchecked")
    private Object readResolve() {
        EnumSet enumSet = EnumSet.noneOf(type);
        enumSet.addAll(Arrays.asList(objects));
        return enumSet;
    }
}
