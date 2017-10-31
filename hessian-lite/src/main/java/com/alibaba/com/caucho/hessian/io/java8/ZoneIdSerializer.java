package com.alibaba.com.caucho.hessian.io.java8;

import com.alibaba.com.caucho.hessian.io.AbstractHessianOutput;
import com.alibaba.com.caucho.hessian.io.AbstractSerializer;

import java.io.IOException;

public class ZoneIdSerializer extends AbstractSerializer {

    private static final ZoneIdSerializer SERIALIZER = new ZoneIdSerializer();

    public static ZoneIdSerializer getInstance() {
        return SERIALIZER;
    }

    @Override
    public void writeObject(Object obj, AbstractHessianOutput out) throws IOException {
        if (obj == null) {
            out.writeNull();
        } else {
            out.writeObject(new ZoneIdHandle(obj));
        }
    }

}
