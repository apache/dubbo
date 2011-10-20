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


/**
 * 日志输出器供给器
 *
 * @author liangfei0201@163.com
 *
 */
public interface LoggerFactorySupport {
	
	/**
	 * 获取日志输出器
	 *
	 * @param key 分类键
	 * @return 日志输出器, 后验条件: 不返回null.
	 */
	Logger getLogger(Class<?> key);

	/**
	 * 获取日志输出器
	 *
	 * @param key 分类键
	 * @return 日志输出器, 后验条件: 不返回null.
	 */
	Logger getLogger(String key);
	
	/**
	 * 设置输出等级
	 * 
	 * @param level
	 */
	void setLevel(Level level);
	
	/**
	 * 
	 * @return
	 */
	Level getLevel();
	
	/**
	 * 
	 * @return
	 */
	File getFile();
	
	/**
	 * 
	 * @param file
	 */
	void setFile(File file);

}