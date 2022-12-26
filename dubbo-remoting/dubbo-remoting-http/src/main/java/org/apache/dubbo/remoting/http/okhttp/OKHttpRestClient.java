package org.apache.dubbo.remoting.http.okhttp;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.dubbo.remoting.http.BaseRestClient;
import org.apache.dubbo.remoting.http.config.HttpClientConfig;

import java.io.IOException;
import java.util.concurrent.TimeUnit;


public class OKHttpRestClient extends BaseRestClient<Request, Response, OkHttpClient> {

    public OKHttpRestClient(HttpClientConfig clientConfig) {
        super(clientConfig);
    }

    @Override
    public Response send(Request message) throws IOException {
        return getClient().newCall(message).execute();
    }

    @Override
    public void close() {
        getClient().connectionPool().evictAll();
    }

    @Override
    public void close(int timeout) {

    }

    @Override
    public boolean isClosed() {
        getClient().retryOnConnectionFailure();
        return false;
    }

    public OkHttpClient createHttpClient(HttpClientConfig httpClientConfig) {
        OkHttpClient client = new OkHttpClient.Builder().
            readTimeout(httpClientConfig.getReadTimeout(), TimeUnit.SECONDS).
            writeTimeout(httpClientConfig.getWriteTimeout(), TimeUnit.SECONDS).
            connectTimeout(httpClientConfig.getConnectTimeout(), TimeUnit.SECONDS).
            build();
        return client;
    }
}
