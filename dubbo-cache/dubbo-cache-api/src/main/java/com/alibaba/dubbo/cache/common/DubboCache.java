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
package com.alibaba.dubbo.cache.common;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class DubboCache {
	
	private String service;
	private String address;
	private String prefix;
	private int maxActive;
	private int maxIdle;
	private int minIdle;
	private int defaultTimeout;
	
	private Map<String, DubboBuffer> bufferMap;
	
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

	public Map<String, DubboBuffer> getBufferMap() {
		return bufferMap;
	}

	public void setBufferMap(Map<String, DubboBuffer> bufferMap) {
		this.bufferMap = bufferMap;
	}
	
	public DubboCache(String cacheData) {
		bufferMap = new HashMap<String, DubboBuffer>();
		try {
			String paramStr = URLDecoder.decode(cacheData, "utf-8");
			String[] paramAry = paramStr.split("&");
	    	for (String param : paramAry) {
	    		String[] pv = param.split("=");
	    		String key = pv[0];
	    		String value = "";
	    		if (pv.length > 1) {
	    			value = pv[1];
	    		}
	    		if (key.equals("service")) {
	    			this.service = value;
	    		}
	    		if (key.equals("address")) {
	    			this.address = value;
	    		}
	    		if (key.equals("prefix")) {
	    			this.prefix = value;
	    		}

	    		if (key.equals("maxActive")) {
	    			this.maxActive = Integer.parseInt(value);
	    		}
	    		if (key.equals("maxIdle")) {
	    			this.maxIdle = Integer.parseInt(value);
	    		}
	    		if (key.equals("minIdle")) {
	    			this.minIdle = Integer.parseInt(value);
	    		}
	    		if (key.equals("defaultTimeout")) {
	    			this.defaultTimeout = Integer.parseInt(value);
	    		}
	    		
	    		if (key.equals("buffer")) {
	    			String[] bufferValue = value.split(";");
	    			for (String bufferString : bufferValue) {
	    				String[] v = bufferString.split("\\|");
		    			DubboBuffer buffer = new DubboBuffer();
		    			buffer.setKey(v[0]);
		    			buffer.setMethod(v[1]);
		    			buffer.setCommand(v[2]);
		    			buffer.setTimeout(Integer.parseInt(v[3]));
		    			bufferMap.put(buffer.getMethod(), buffer);
	    			}
	    		}
	    	}
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

}
