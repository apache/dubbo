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
package com.alibaba.dubbo.common.logger.jdk;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.LogManager;

import com.alibaba.dubbo.common.logger.Level;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerAdapter;

public class JdkLoggerAdapter implements LoggerAdapter {
	
	private static final String GLOBAL_LOGGER_NAME = "global";

    private File file;

	public JdkLoggerAdapter() {
		try {
			InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("logging.properties");
			if (in != null) {
				LogManager.getLogManager().readConfiguration(in);
			} else {
				System.err.println("No such logging.properties in classpath for jdk logging config!");
			}
		} catch (Throwable t) {
			System.err.println("Failed to load logging.properties in classpath for jdk logging config, cause: " + t.getMessage());
		}
		try {
			Handler[] handlers = java.util.logging.Logger.getLogger(GLOBAL_LOGGER_NAME).getHandlers();
			for (Handler handler : handlers) {
				if (handler instanceof FileHandler) {
					FileHandler fileHandler = (FileHandler)handler;
					Field field = fileHandler.getClass().getField("files");
					File[] files =  (File[])field.get(fileHandler);
					if (files != null && files.length > 0) {
						file = files[0];
					}
				}
			}
		} catch (Throwable t) {
		}
	}

	public Logger getLogger(Class<?> key) {
		return new JdkLogger(java.util.logging.Logger.getLogger(key == null ? "" : key.getName()));
	}

	public Logger getLogger(String key) {
		return new JdkLogger(java.util.logging.Logger.getLogger(key));
	}

	public void setLevel(Level level) {
		java.util.logging.Logger.getLogger(GLOBAL_LOGGER_NAME).setLevel(toJdkLevel(level));
	}

	public Level getLevel() {
		return fromJdkLevel(java.util.logging.Logger.getLogger(GLOBAL_LOGGER_NAME).getLevel());
	}

	public File getFile() {
		return file;
	}

	private static java.util.logging.Level toJdkLevel(Level level) {
		if (level == Level.ALL)
			return java.util.logging.Level.ALL;
		if (level == Level.TRACE)
			return java.util.logging.Level.FINER;
		if (level == Level.DEBUG)
			return java.util.logging.Level.FINE;
		if (level == Level.INFO)
			return java.util.logging.Level.INFO;
		if (level == Level.WARN)
			return java.util.logging.Level.WARNING;
		if (level == Level.ERROR)
			return java.util.logging.Level.SEVERE;
		// if (level == Level.OFF)
			return java.util.logging.Level.OFF;
	}

	private static Level fromJdkLevel(java.util.logging.Level level) {
		if (level == java.util.logging.Level.ALL)
			return Level.ALL;
		if (level == java.util.logging.Level.FINER)
			return Level.TRACE;
		if (level == java.util.logging.Level.FINE)
			return Level.DEBUG;
		if (level == java.util.logging.Level.INFO)
			return Level.INFO;
		if (level == java.util.logging.Level.WARNING)
			return Level.WARN;
		if (level == java.util.logging.Level.SEVERE)
			return Level.ERROR;
		// if (level == java.util.logging.Level.OFF)
			return Level.OFF;
	}

    public void setFile(File file) {
        
    }

}