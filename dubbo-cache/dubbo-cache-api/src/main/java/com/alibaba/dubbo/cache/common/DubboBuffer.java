package com.alibaba.dubbo.cache.common;

public class DubboBuffer {

	private String key;
	private String method;
	private String command;
	private int timeout;
	
	private String []keys;
	
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
		keys = key.split(",");
	}
	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public String getCommand() {
		return command;
	}
	public void setCommand(String command) {
		this.command = command;
	}
	public int getTimeout() {
		return timeout;
	}
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
	
	public String[] getKeys() {
		return keys;
	}

	
}
