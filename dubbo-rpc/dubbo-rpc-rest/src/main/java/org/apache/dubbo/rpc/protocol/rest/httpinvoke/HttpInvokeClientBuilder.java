package org.apache.dubbo.rpc.protocol.rest.httpinvoke;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.remoting.http.RestClient;
import org.apache.dubbo.rpc.protocol.rest.ReferenceCountedClient;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

public class HttpInvokeClientBuilder {


    @SuppressWarnings("unchecked")
    public static <T> T build(Map<Method, RestMethodMetadata> metadataMap, URL url, Class<T> service, ReferenceCountedClient<? extends RestClient> restClient) {
        return (T) Proxy.newProxyInstance(service.getClassLoader(), new Class[]{service},
            new HttpInvokeInvocationHandler(metadataMap, url,restClient));
    }

}
