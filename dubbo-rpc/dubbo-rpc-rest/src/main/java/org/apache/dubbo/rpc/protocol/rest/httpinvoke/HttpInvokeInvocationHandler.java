package org.apache.dubbo.rpc.protocol.rest.httpinvoke;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.remoting.http.RestClient;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.rest.ReferenceCountedClient;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.*;
import org.apache.dubbo.rpc.protocol.rest.request.convert.RequestConvert;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HttpInvokeInvocationHandler implements InvocationHandler {
    private static final RequestConvert requestConvert = FrameworkModel.defaultModel().getExtensionLoader(RequestConvert.class).getAdaptiveExtension();

    private static Set<HttpConnectionPreBuildIntercept> httpConnectionPreBuildIntercepts =
        ApplicationModel.defaultModel().getExtensionLoader(HttpConnectionPreBuildIntercept.class).getSupportedExtensionInstances();

    private final Map<Method, RestMethodMetadata> methodRestMethodMetadataMap;
    private final String address;
    private final ReferenceCountedClient<? extends RestClient> refClient;
    private URL url;

    public HttpInvokeInvocationHandler(Map<Method, RestMethodMetadata> metadataMap, URL url, ReferenceCountedClient<? extends RestClient> restClient) {
        this.methodRestMethodMetadataMap = metadataMap;
        this.url = url;
        this.address = url.getAddress();
        this.refClient = restClient;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        RestMethodMetadata restMethodMetadata = methodRestMethodMetadataMap.get(method);

        RequestTemplate requestTemplate = new RequestTemplate(restMethodMetadata.getRequest());


        requestTemplate.setAddress(address);


        HttpConnectionConfig connectionConfig = new HttpConnectionConfig();

        HttpConnectionCreateContext httpConnectionCreateContext = createBuildContext(requestTemplate,
            connectionConfig,
            restMethodMetadata, Arrays.asList(args));

        for (HttpConnectionPreBuildIntercept intercept : httpConnectionPreBuildIntercepts) {
            // TODO add   attachment
            intercept.intercept(httpConnectionCreateContext);
        }


        RequestConvert nettyRequestConvert = requestConvert.createRequestConvert(url);

        return nettyRequestConvert.request(requestTemplate);

        // TODO deal with response

    }

    private static HttpConnectionCreateContext createBuildContext(RequestTemplate requestTemplate,
                                                                  HttpConnectionConfig connectionConfig,
                                                                  RestMethodMetadata restMethodMetadata, List<Object> rags) {
        HttpConnectionCreateContext httpConnectionCreateContext = new HttpConnectionCreateContext();
        httpConnectionCreateContext.setConnectionConfig(connectionConfig);
        httpConnectionCreateContext.setRequestTemplate(requestTemplate);
        httpConnectionCreateContext.setRestMethodMetadata(restMethodMetadata);
        httpConnectionCreateContext.setMethodRealArgs(rags);
        return httpConnectionCreateContext;
    }
}
