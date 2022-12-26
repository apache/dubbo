package org.apache.dubbo.remoting.http.config;

public class HttpClientConfig {
    private int readTimeout = 30;
    private int writeTimeout = 30;
    private int connectTimeout = 30;

    private int HTTP_CLIENT_CONNECTION_MANAGER_MAX_PER_ROUTE = 20;
    private int HTTP_CLIENT_CONNECTION_MANAGER_MAX_TOTAL = 20;
    private int HTTPCLIENT_KEEP_ALIVE_DURATION = 30 * 1000;
    private int HTTP_CLIENT_CONNECTION_MANAGER_CLOSE_WAIT_TIME_MS = 1000;
    private int HTTP_CLIENT_CONNECTION_MANAGER_CLOSE_IDLE_TIME_S = 30;


    public HttpClientConfig() {
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getWriteTimeout() {
        return writeTimeout;
    }

    public void setWriteTimeout(int writeTimeout) {
        this.writeTimeout = writeTimeout;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }
}
