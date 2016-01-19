/**
 * Copyright 2006-2014 handu.com.
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
package com.alibaba.dubbo.container.javaconfig;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.container.Container;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * JavaConfigContainer. (SPI, Singleton, ThreadSafe)
 *
 * @author Jinkai.Ma
 */
public class JavaConfigContainer implements Container {

    private static final Logger logger = LoggerFactory.getLogger(JavaConfigContainer.class);

    public static final String SPRING_JAVACONFIG = "dubbo.spring.javaconfig";

    public static final String DEFAULT_SPRING_JAVACONFIG = "dubbo.spring.javaconfig";

    static AnnotationConfigApplicationContext context;

    public static AnnotationConfigApplicationContext getContext() {
        return context;
    }

    public void start() {
        String configPath = ConfigUtils.getProperty(SPRING_JAVACONFIG);
        if (configPath == null || configPath.length() == 0) {
            configPath = DEFAULT_SPRING_JAVACONFIG;
        }
        context = new AnnotationConfigApplicationContext(configPath);
        context.start();
    }

    public void stop() {
        try {
            if (context != null) {
                context.stop();
                context.close();
                context = null;
            }
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
    }

}
