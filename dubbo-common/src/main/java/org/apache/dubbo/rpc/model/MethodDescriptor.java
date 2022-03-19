package org.apache.dubbo.rpc.model;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

public interface MethodDescriptor {

    String getMethodName();

    String getParamDesc();

    /**
     * duplicate filed as paramDesc, but with different format.
     */
    String[] getCompatibleParamSignatures();

    Class<?>[] getParameterClasses();

    Class<?> getReturnClass();

    Type[] getReturnTypes();

    RpcType getRpcType();

    boolean isGeneric();

    /**
     * Only available for ReflectionMethod
     *
     * @return method
     */
    Method getMethod();

    void addAttribute(String key, Object value);

    Object getAttribute(String key);

    enum RpcType {
        UNARY, CLIENT_STREAM, SERVER_STREAM, BI_STREAM
    }
}
