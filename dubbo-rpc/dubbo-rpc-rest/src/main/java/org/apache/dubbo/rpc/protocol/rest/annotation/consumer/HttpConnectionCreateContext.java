package org.apache.dubbo.rpc.protocol.rest.annotation.consumer;

import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.metadata.rest.ServiceRestMetadata;

import java.util.List;

public class HttpConnectionCreateContext {

    private RequestTemplate requestTemplate;
    private HttpConnectionConfig connectionConfig;
    private RestMethodMetadata restMethodMetadata;
    private List<Object> methodRealArgs;

    public HttpConnectionCreateContext() {
    }


    public HttpConnectionCreateContext(RequestTemplate requestTemplate,
                                       HttpConnectionConfig connectionConfig,
                                       List<Object> methodRealArgs) {
        this.requestTemplate = requestTemplate;
        this.connectionConfig = connectionConfig;
        this.methodRealArgs = methodRealArgs;
    }

    public void setRequestTemplate(RequestTemplate requestTemplate) {
        this.requestTemplate = requestTemplate;
    }

    public void setConnectionConfig(HttpConnectionConfig connectionConfig) {
        this.connectionConfig = connectionConfig;
    }


    public RequestTemplate getRequestTemplate() {
        return requestTemplate;
    }

    public HttpConnectionConfig getConnectionConfig() {
        return connectionConfig;
    }

    public ServiceRestMetadata getServiceRestMetadata() {
        return restMethodMetadata.getServiceRestMetadata();
    }

    public RestMethodMetadata getRestMethodMetadata() {
        return restMethodMetadata;
    }

    public void setRestMethodMetadata(RestMethodMetadata restMethodMetadata) {
        this.restMethodMetadata = restMethodMetadata;
    }

    public List<Object> getMethodRealArgs() {
        return methodRealArgs;
    }

    public void setMethodRealArgs(List<Object> methodRealArgs) {
        this.methodRealArgs = methodRealArgs;
    }
}
