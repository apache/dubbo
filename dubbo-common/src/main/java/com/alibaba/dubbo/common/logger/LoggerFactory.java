/*
 * Copyright 1999-2011 Alibaba Group.
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
package com.alibaba.dubbo.common.logger;

import java.io.File;

import com.alibaba.dubbo.common.logger.support.FailsafeLogger;
import com.alibaba.dubbo.common.logger.support.JdkLoggerFactory;
import com.alibaba.dubbo.common.logger.support.Log4jLoggerFactory;

/**
 * 日志输出器工厂
 * 
 * @author william.liangf
 */
public class LoggerFactory {

	private LoggerFactory() {
	}

	private static volatile LoggerFactorySupport LOGGER_FACTORY;

	// 查找常用的日志框架
	static {
		try {
            setLoggerFactory(new Log4jLoggerFactory());
        } catch (Throwable e1) {
        	setLoggerFactory(new JdkLoggerFactory());
        }
	}

	/**
	 * 设置日志输出器供给器
	 * 
	 * @param loggerFactory
	 *            日志输出器供给器
	 */
	public static void setLoggerFactory(LoggerFactorySupport loggerFactory) {
		if (loggerFactory != null) {
			Logger logger = loggerFactory.getLogger(LoggerFactory.class.getName());
			logger.info("using logger: " + loggerFactory.getClass().getName());
			LoggerFactory.LOGGER_FACTORY = loggerFactory;
		}
	}

	/**
	 * 获取日志输出器
	 * 
	 * @param key
	 *            分类键
	 * @return 日志输出器, 后验条件: 不返回null.
	 */
	public static Logger getLogger(Class<?> key) {
		return new FailsafeLogger(LOGGER_FACTORY.getLogger(key));
	}

	/**
	 * 获取日志输出器
	 * 
	 * @param key
	 *            分类键
	 * @return 日志输出器, 后验条件: 不返回null.
	 */
	public static Logger getLogger(String key) {
		return new FailsafeLogger(LOGGER_FACTORY.getLogger(key));
	}
	
	/**
	 * 动态设置输出日志级别
	 * 
	 * @param level 日志级别
	 */
	public static void setLevel(Level level) {
		LOGGER_FACTORY.setLevel(level);
	}

	/**
	 * 获取日志级别
	 * 
	 * @return 日志级别
	 */
	public static Level getLevel() {
		return LOGGER_FACTORY.getLevel();
	}
	
	/**
	 * 获取日志文件
	 * 
	 * @return 日志文件
	 */
	public static File getFile() {
		return LOGGER_FACTORY.getFile();
	}

}