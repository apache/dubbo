package com.alibaba.dubbo.config.model;

import java.lang.reflect.Method;

public class ProviderMethodModel {
    private transient final Method method;
    private final String methodName;
    private final String[] methodArgTypes;
    private final String serviceName;


    public ProviderMethodModel(Method method, String serviceName) {
        this.method = method;
        this.serviceName = serviceName;
        this.methodName = method.getName();
        this.methodArgTypes = getArgTypes(method);
    }

    public Method getMethod() {
        return method;
    }

    public String getMethodName() {
        return methodName;
    }

    public String[] getMethodArgTypes() {
        return methodArgTypes;
    }

    public String getServiceName() {
        return serviceName;
    }

    private static String[] getArgTypes(Method method) {
        String[] methodArgTypes = new String[0];
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length > 0) {
            methodArgTypes = new String[parameterTypes.length];
            int index = 0;
            for (Class<?> paramType : parameterTypes) {
                methodArgTypes[index++] = paramType.getName();
            }
        }
        return methodArgTypes;
    }
}
