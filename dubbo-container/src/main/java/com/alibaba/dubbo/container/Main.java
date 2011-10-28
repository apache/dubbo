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
package com.alibaba.dubbo.container;

import com.alibaba.dubbo.common.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;

/**
 * Main
 * 
 * @author william.liangf
 */
public class Main {
    
    private static final Logger logger            = LoggerFactory.getLogger(Main.class);
    
    public static final String CONTAINER_TYPE_KEY = "container.type";
    
    public static void main(String[] args) {
        String type = null;
        if(args.length > 0 && args[0].length() > 0) {
            type = args[0];
        }
        if(null == type) {
            type = System.getProperty(CONTAINER_TYPE_KEY);
            if(type != null && type.length() > 0)
                logger.info("Get Container type from system property " + CONTAINER_TYPE_KEY + ": " + type);
        }
        
        final Container container;
        if(null == type || type.length() == 0) {
            container = ExtensionLoader.getExtensionLoader(Container.class).getDefaultExtension();
            logger.info("Use default container type(" + ExtensionLoader.getExtensionLoader(Container.class).getDefaultExtensionName()
            		+ ") to run dubbo serivce.");
        }
        else {
            container = ExtensionLoader.getExtensionLoader(Container.class).getExtension(type);
            logger.info("Use container type(" + type + ") to run dubbo serivce.");
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                container.stop();
            }
        });
        container.start();
        
        synchronized (Main.class) {
            for (;;) {
                try {
                    Main.class.wait();
                } catch (Throwable e) {
                }
            }
        }
    }
    
}