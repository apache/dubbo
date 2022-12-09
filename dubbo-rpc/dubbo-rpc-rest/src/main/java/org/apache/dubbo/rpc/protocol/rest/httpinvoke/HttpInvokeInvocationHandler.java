package org.apache.dubbo.rpc.protocol.rest.httpinvoke;

import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.HttpConnectionConfig;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.HttpURLConnectionBuilder;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.RequestTemplate;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.util.Map;

public class HttpInvokeInvocationHandler implements InvocationHandler {

    private Map<Method, RestMethodMetadata> methodRestMethodMetadataMap;
    private String address;

    public HttpInvokeInvocationHandler(Map<Method, RestMethodMetadata> methodRestMethodMetadataMap, String address) {
        this.methodRestMethodMetadataMap = methodRestMethodMetadataMap;
        this.address = address;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        RestMethodMetadata restMethodMetadata = methodRestMethodMetadataMap.get(method);

        RequestTemplate requestTemplate = new RequestTemplate();


        requestTemplate.setAddress(address);
        HttpConnectionConfig connectionConfig = null;

        HttpURLConnection build = HttpURLConnectionBuilder.build(requestTemplate, connectionConfig, restMethodMetadata);


        // TODO deal with response


        return null;
    }
}
