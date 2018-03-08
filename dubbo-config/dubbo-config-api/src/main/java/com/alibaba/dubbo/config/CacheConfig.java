/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.config;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import com.alibaba.dubbo.config.support.Parameter;

/**
 * CacheConfig
 * 
 * @author 
 * @export
 */
public class CacheConfig extends AbstractConfig {

	private static final long serialVersionUID = 5679092108207708658L;
	
    protected String server;
	protected String service;
	protected String address;
	protected String prefix;
	/*protected String user;
	protected String password;*/
	protected int maxActive;
	protected int maxIdle;
	protected int minIdle;
	protected int defaultTimeout;
	protected List<BufferConfig> bufferList;
	private transient volatile boolean exported;
	
	public String getServer() {
		return server;
	}
	public void setServer(String server) {
		this.server = server;
	}
	public String getService() {
		return service;
	}
	public void setService(String service) {
		this.service = service;
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
	/*public String getUser() {
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
	}*/
	public int getMaxActive() {
		return maxActive;
	}
	public void setMaxActive(int maxActive) {
		this.maxActive = maxActive;
	}
	public int getMaxIdle() {
		return maxIdle;
	}
	public void setMaxIdle(int maxIdle) {
		this.maxIdle = maxIdle;
	}
	public int getMinIdle() {
		return minIdle;
	}
	public void setMinIdle(int minIdle) {
		this.minIdle = minIdle;
	}
	public int getDefaultTimeout() {
		return defaultTimeout;
	}
	public void setDefaultTimeout(int defaultTimeout) {
		this.defaultTimeout = defaultTimeout;
	}
	public List<BufferConfig> getBufferList() {
		return bufferList;
	}
	public void setBufferList(List<BufferConfig> bufferList) {
		this.bufferList = bufferList;
	}
	
	public String toUTFString() {
		StringBuffer sb = new StringBuffer();
		sb.append("service=");
		sb.append(service);
		sb.append("&address=");
		sb.append(address);
		sb.append("&prefix=");
		if (prefix == null) {
			prefix = "";
		}
		sb.append(prefix);
		
		sb.append("&maxActive=");
		sb.append(maxActive);
		sb.append("&maxIdle=");
		sb.append(maxIdle);
		sb.append("&minIdle=");
		sb.append(minIdle);
		sb.append("&defaultTimeout=");
		sb.append(defaultTimeout);
		
		sb.append("&buffer=");
		String bufferStr = "";
		for (BufferConfig buffer : bufferList) {
			try {
				String key = buffer.getKey();
				String method = buffer.getMethod();
				/*Class<?> serviceClazz = Class.forName(service);
				String[] methodParamNames = getMethodParamNames(serviceClazz, method);
				Map<String, Integer> methodParamMap = new HashMap<String, Integer>();
				for (int i=0; i<methodParamNames.length; i++) {
					String methodParamName = methodParamNames[i];
					methodParamMap.put(methodParamName, i+1);
				}
				key = getCacheKey(key, methodParamMap);*/
				String bcs = key + "|" + method + "|" + buffer.getCommand() + "|" + buffer.getTimeout();
				bufferStr += (";" + bcs);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		if (bufferStr.length() > 0) {
			bufferStr = bufferStr.substring(1);
		}
		sb.append(bufferStr);
		try {
			String encodeStr = URLEncoder.encode(sb.toString(), "utf-8");
			return encodeStr;
		} catch (UnsupportedEncodingException e) {
			return sb.toString();
		}
	}
	
	@Parameter(excluded = true)
    public boolean isExported() {
		return exported;
	}
	
	public void doExport() {
		exported = true;
	}	

}
