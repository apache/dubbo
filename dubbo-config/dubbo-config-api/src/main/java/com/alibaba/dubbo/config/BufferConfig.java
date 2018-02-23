package com.alibaba.dubbo.config;

/**
 * CacheConfig
 * 
 * @author laocoon.kong
 * @export
 */
public class BufferConfig extends AbstractConfig {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3655250290384872271L;
	
	protected String key;
	protected String method;
	protected String command;
	protected int timeout;
	
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
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

}
