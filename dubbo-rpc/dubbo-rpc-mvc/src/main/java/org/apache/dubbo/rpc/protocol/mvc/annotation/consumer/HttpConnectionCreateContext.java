package org.apache.dubbo.rpc.protocol.mvc.annotation.consumer;

import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.metadata.rest.ServiceRestMetadata;

public class HttpConnectionCreateContext {

    private RequestTemplate requestTemplate;
    private HttpConnectionConfig connectionConfig;
    private ServiceRestMetadata serviceRestMetadata;
    private RestMethodMetadata restMethodMetadata;

    public HttpConnectionCreateContext() {
    }


    public HttpConnectionCreateContext(RequestTemplate requestTemplate,
                                       HttpConnectionConfig connectionConfig,
                                       ServiceRestMetadata restMethodMetadata) {
        this.requestTemplate = requestTemplate;
        this.connectionConfig = connectionConfig;
        this.serviceRestMetadata = restMethodMetadata;
    }

    public void setRequestTemplate(RequestTemplate requestTemplate) {
        this.requestTemplate = requestTemplate;
    }

    public void setConnectionConfig(HttpConnectionConfig connectionConfig) {
        this.connectionConfig = connectionConfig;
    }

    public void setServiceRestMetadata(ServiceRestMetadata serviceRestMetadata) {
        this.serviceRestMetadata = serviceRestMetadata;
    }

    public RequestTemplate getRequestTemplate() {
        return requestTemplate;
    }

    public HttpConnectionConfig getConnectionConfig() {
        return connectionConfig;
    }

    public ServiceRestMetadata getServiceRestMetadata() {
        return serviceRestMetadata;
    }

    public RestMethodMetadata getRestMethodMetadata() {
        return restMethodMetadata;
    }

    public void setRestMethodMetadata(RestMethodMetadata restMethodMetadata) {
        this.restMethodMetadata = restMethodMetadata;
    }
}
