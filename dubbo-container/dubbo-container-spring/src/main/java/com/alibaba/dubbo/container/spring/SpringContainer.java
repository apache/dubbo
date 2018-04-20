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
package com.alibaba.dubbo.container.spring;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.container.Container;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * SpringContainer. (SPI, Singleton, ThreadSafe)
 *
 * Spring 容器实现类
 */
public class SpringContainer implements Container {

    private static final Logger logger = LoggerFactory.getLogger(SpringContainer.class);

    /**
     * Spring 配置属性 KEY
     */
    public static final String SPRING_CONFIG = "dubbo.spring.config";
    /**
     * 默认配置文件地址
     */
    public static final String DEFAULT_SPRING_CONFIG = "classpath*:META-INF/spring/*.xml";
    /**
     * Spring Context
     *
     * 静态属性，全局唯一
     */
    static ClassPathXmlApplicationContext context;

    public static ClassPathXmlApplicationContext getContext() {
        return context;
    }

    @Override
    public void start() {
        // 获得 Spring 配置文件的地址
        String configPath = ConfigUtils.getProperty(SPRING_CONFIG);
        if (configPath == null || configPath.length() == 0) {
            configPath = DEFAULT_SPRING_CONFIG;
        }
        // 创建 Spring Context 对象
        context = new ClassPathXmlApplicationContext(configPath.split("[,\\s]+"));
        // 启动 Spring Context ，会触发 ContextStartedEvent 事件
        context.start();
    }

    @Override
    public void stop() {
        try {
            if (context != null) {
                // 停止 Spring Context ，会触发 ContextStoppedEvent 事件。
                context.stop();
                // 关闭 Spring Context ，会触发 ContextClosedEvent 事件。
                context.close();
                context = null;
            }
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
    }

}