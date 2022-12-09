package org.apache.dubbo.rpc.protocol.rest.httpinvoke;

import org.apache.dubbo.metadata.rest.RestMethodMetadata;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

public class HttpInvokeClientBuilder<T> {


    public static <T> T build(Map<Method, RestMethodMetadata> methodRestMethodMetadataMap, String address, Class<T> service) {
        return (T) Proxy.newProxyInstance(service.getClass().getClassLoader(), new Class[]{service},
            new HttpInvokeInvocationHandler(methodRestMethodMetadataMap, address));
    }

}
