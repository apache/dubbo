package org.apache.dubbo.remoting.http;


import org.apache.dubbo.remoting.http.config.HttpClientConfig;

public abstract class BaseRestClient<REQ, RES, CLIENT> implements RestClient<REQ, RES> {

    protected CLIENT client;

    protected HttpClientConfig clientConfig;

    public BaseRestClient(HttpClientConfig clientConfig) {
        this.clientConfig = clientConfig;
        client = createHttpClient(clientConfig);
    }

    protected abstract CLIENT createHttpClient(HttpClientConfig clientConfig);


    public HttpClientConfig getClientConfig() {
        return clientConfig;
    }

    public CLIENT getClient() {
        return client;
    }
}
