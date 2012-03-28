/*
 * Copyright 1999-2101 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.registry.support;

import com.alibaba.dubbo.common.Version;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.registry.Registry;

public class RegistryLogger implements Logger {
	
	private final Registry registry;
	
	private final Logger logger;

	public RegistryLogger(Registry registry, Logger logger) {
		if (registry == null)
			throw new IllegalArgumentException("registry == null");
		if (logger == null)
			throw new IllegalArgumentException("logger == null");
		this.registry = registry;
		this.logger = logger;
	}

	private String getErrorString(Throwable e) {
		return (e == null ? "" : e.getMessage()) + getEnvString();
    }
	
	private String getErrorString(String msg) {
		return (msg == null ? "" : msg) + getEnvString();
    }
	
    private String getErrorString(String msg, Throwable e) {
    	return (msg == null ? "" : msg) + getEnvString() + (e == null ? "" :  ", cause: " + e.getMessage());
    }
    
    private String getEnvString() {
    	return " from " + NetUtils.getLocalHost() + " to registry " + registry.getUrl().getAddress() + " use dubbo " + Version.getVersion();
    }

	public void trace(String msg) {
		logger.trace(getErrorString(msg));
	}

	public void trace(Throwable e) {
		logger.trace(getErrorString(e), e);
	}

	public void trace(String msg, Throwable e) {
		logger.trace(getErrorString(msg, e), e);
	}

	public void debug(String msg) {
		logger.debug(getErrorString(msg));
	}

	public void debug(Throwable e) {
		logger.debug(getErrorString(e), e);
	}

	public void debug(String msg, Throwable e) {
		logger.debug(getErrorString(msg, e), e);
	}

	public void info(String msg) {
		logger.info(getErrorString(msg));
	}

	public void info(Throwable e) {
		logger.info(getErrorString(e), e);
	}

	public void info(String msg, Throwable e) {
		logger.info(getErrorString(msg, e), e);
	}

	public void warn(String msg) {
		logger.warn(getErrorString(msg));
	}

	public void warn(Throwable e) {
		logger.warn(getErrorString(e), e);
	}

	public void warn(String msg, Throwable e) {
		logger.warn(getErrorString(msg, e), e);
	}

	public void error(String msg) {
		logger.error(getErrorString(msg));
	}

	public void error(Throwable e) {
		logger.error(getErrorString(e), e);
	}

	public void error(String msg, Throwable e) {
		logger.error(getErrorString(msg, e), e);
	}

	public boolean isTraceEnabled() {
		return logger.isTraceEnabled();
	}

	public boolean isDebugEnabled() {
		return logger.isDebugEnabled();
	}

	public boolean isInfoEnabled() {
		return logger.isInfoEnabled();
	}

	public boolean isWarnEnabled() {
		return logger.isWarnEnabled();
	}

    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

}