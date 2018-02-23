package com.alibaba.dubbo.config.spring;

public class CacheServerBean {
	
	private String id;
	private String protocol;
	private String address;
	private String prefix;
	private String user;
	private String password;
	private String maxactive;
	private String maxidle;
	private String minidle;
	private String timeout;
	
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getProtocol() {
		return protocol;
	}
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public String getPrefix() {
		return prefix;
	}
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getMaxactive() {
		return maxactive;
	}
	public void setMaxactive(String maxactive) {
		this.maxactive = maxactive;
	}
	public String getMaxidle() {
		return maxidle;
	}
	public void setMaxidle(String maxidle) {
		this.maxidle = maxidle;
	}
	public String getMinidle() {
		return minidle;
	}
	public void setMinidle(String minidle) {
		this.minidle = minidle;
	}
	public String getTimeout() {
		return timeout;
	}
	public void setTimeout(String timeout) {
		this.timeout = timeout;
	}
	
	

}
