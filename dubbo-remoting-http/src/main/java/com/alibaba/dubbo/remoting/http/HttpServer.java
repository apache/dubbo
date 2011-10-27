package com.alibaba.dubbo.remoting.http;

public interface HttpServer {

	void start();
	
	void stop();
	
	int getPort();

}
