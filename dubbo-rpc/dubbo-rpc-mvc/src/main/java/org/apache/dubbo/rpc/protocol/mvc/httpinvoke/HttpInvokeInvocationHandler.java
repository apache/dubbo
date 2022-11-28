package org.apache.dubbo.rpc.protocol.mvc.httpinvoke;

import org.apache.dubbo.metadata.rest.RestMethodMetadata;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class HttpInvokeInvocationHandler implements InvocationHandler {

    private Map<Method, RestMethodMetadata> methodRestMethodMetadataMap = new HashMap<>();

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        RestMethodMetadata restMethodMetadata = methodRestMethodMetadataMap.get(method);

        // TODO create request
        return null;
    }
}
