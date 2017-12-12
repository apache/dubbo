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

import com.alibaba.dubbo.common.extension.SPI;

import java.io.File;

/**
 * 日志输出器供给器
 *
 * @author william.liangf
 */
@SPI
public interface LoggerAdapter {

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
     * 获取当前日志等级
     *
     * @return 当前日志等级
     */
    Level getLevel();

    /**
     * 设置输出等级
     *
     * @param level 输出等级
     */
    void setLevel(Level level);

    /**
     * 获取当前日志文件
     *
     * @return 当前日志文件
     */
    File getFile();

    /**
     * 设置输出日志文件
     *
     * @param file 输出日志文件
     */
    void setFile(File file);

}