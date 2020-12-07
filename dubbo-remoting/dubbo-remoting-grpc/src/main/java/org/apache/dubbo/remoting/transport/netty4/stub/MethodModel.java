package org.apache.dubbo.remoting.transport.netty4.stub;

import java.lang.reflect.Method;

import org.apache.dubbo.remoting.transport.netty4.marshaller.Marshaller;

public interface MethodModel {

    Object invoke(Object... arg);

    String getName();

    String getService();

    Method getReflect();

    Marshaller getReqMarshaller(String marshallerType);

    Marshaller getRespMarshaller(String marshallerType);

}
