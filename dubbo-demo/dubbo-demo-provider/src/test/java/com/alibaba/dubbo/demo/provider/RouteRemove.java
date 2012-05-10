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
package com.alibaba.dubbo.demo.provider;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.demo.DemoService;
import com.alibaba.dubbo.registry.Registry;
import com.alibaba.dubbo.registry.RegistryFactory;

/**
 * RegistryContainer
 * 
 * @author william.liangf
 */
public class RouteRemove {

    private static final Logger logger = LoggerFactory.getLogger(RouteRemove.class);

    public static final String REGISTRY_ADDRESS = "dubbo.registry.address";

    private final RegistryFactory registryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class).getAdaptiveExtension();

    private Registry registry;

    public Registry getRegistry() {
        return registry;
    }
    
    public static void main(String[] args) throws Exception {
        RouteRemove c = new RouteRemove();
        c.start();
        Thread.sleep(500);
        c.stop();
    }

    public void start() {
        String url = ConfigUtils.getProperty(REGISTRY_ADDRESS);
        if (url == null || url.length() == 0) {
            throw new IllegalArgumentException("Please set java start argument: -D" + REGISTRY_ADDRESS + "=zookeeper://127.0.0.1:2181");
        }
        URL registryUrl = URL.valueOf(url).addParameter(Constants.CHECK_KEY, String.valueOf(false));
        registry = registryFactory.getRegistry(registryUrl);
        URL routeUrl = URL.valueOf("condition://" + NetUtils.getLocalHost() + "/" + DemoService.class.getName() + "?category=routers&dynamic=false&rule=" + URL.encode("host=" + NetUtils.getLocalHost() + " => host=" + NetUtils.getLocalHost()));
        registry.unregister(routeUrl);
    }

    public void stop() {
        try {
            registry.destroy();
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
    }

}