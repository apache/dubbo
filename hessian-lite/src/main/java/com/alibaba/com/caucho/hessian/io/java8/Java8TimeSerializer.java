package com.alibaba.com.caucho.hessian.io.java8;

import java.io.IOException;
import java.lang.reflect.Constructor;

import com.alibaba.com.caucho.hessian.io.AbstractHessianOutput;
import com.alibaba.com.caucho.hessian.io.AbstractSerializer;

public class Java8TimeSerializer<T> extends AbstractSerializer {

    //handle 具体类型
    private Class<T> handleType;

    private Java8TimeSerializer(Class<T> handleType) {
        this.handleType = handleType;
    }

    public static <T> Java8TimeSerializer<T> create(Class<T> handleType) {
        return new Java8TimeSerializer<T>(handleType);
    }

    @Override
    public void writeObject(Object obj, AbstractHessianOutput out) throws IOException {
        if(obj == null) {
            out.writeNull();
            return;
        }
        
        T handle = null;
        try {
            Constructor<T> constructor = handleType.getConstructor(Object.class);
            handle = constructor.newInstance(obj);
        } catch (Exception e) {
            throw new RuntimeException("the class :" + handleType.getName() + " construct failed:" + e.getMessage(), e);
        }
        
        out.writeObject(handle);
    }
}
