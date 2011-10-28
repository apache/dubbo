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

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;

/**
 * Main
 * 
 * @author william.liangf
 */
public class Main {

    private static final Logger logger    = LoggerFactory.getLogger(Main.class);

    public static final String  CONTAINER = "container";

    public static void main(String[] args) {
        String type = null;
        if(args.length > 0 && args[0].length() > 0) {
            type = args[0];
        }
        if(null == type) {
            type = System.getProperty(CONTAINER);
            if(type != null && type.length() > 0)
                logger.info("Get Container type from system property " + CONTAINER + ": " + type);
        }
        
        final Container[] containers;
        if(null == type || type.length() == 0) {
            containers = new Container[] {ExtensionLoader.getExtensionLoader(Container.class).getDefaultExtension()};
            logger.info("Use default container type(" + ExtensionLoader.getExtensionLoader(Container.class).getDefaultExtensionName() + ") to run dubbo serivce.");
        } else {
            String[] types = Constants.COMMA_SPLIT_PATTERN.split(type);
            containers = new Container[types.length];
            for (int i = 0; i < types.length; i ++) {
                containers[i] = ExtensionLoader.getExtensionLoader(Container.class).getExtension(types[i]);
            }
            logger.info("Use container type(" + type + ") to run dubbo serivce.");
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                for (Container container : containers) {
                    try {
                        container.stop();
                    } catch (Throwable t) {
                        logger.error(t.getMessage(), t);
                    }
                }
            }
        });
        for (Container container : containers) {
            container.start();
        }
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