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
package com.alibaba.dubbo.container.properties;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import com.alibaba.dubbo.common.Extension;
import com.alibaba.dubbo.container.Container;

/**
 * PropertiesContainer
 * 
 * @author william.liangf
 */
@Extension("properties")
public class PropertiesContainer implements Container {

    public static final String PROPERTIES_FILE         = "properties.file";

    public static final String DEFAULT_PROPERTIES_FILE = "system.properties";

    public void start() {
        String file = System.getProperty(PROPERTIES_FILE, DEFAULT_PROPERTIES_FILE);
        Properties properties = new Properties();
        try {
            properties.load(Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(file));
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                System.setProperty((String) entry.getKey(), (String) entry.getValue());
            }
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public void stop() {
    }

}
