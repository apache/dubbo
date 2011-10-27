package com.alibaba.dubbo.remoting.http;

public class ServletHttpServer implements HttpServer {
    
    private final int port;

	public ServletHttpServer(int port){
        this.port = port;
    }

    public void start() {
	}

	public void stop() {
	}

    public int getPort() {
        return port;
    }

}
