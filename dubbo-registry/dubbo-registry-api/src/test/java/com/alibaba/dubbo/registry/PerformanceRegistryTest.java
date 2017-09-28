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
package com.alibaba.dubbo.registry;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.NetUtils;

import junit.framework.TestCase;
import org.junit.Test;

/**
 * RegistryPerformanceTest
 *
 * @author william.liangf
 */
public class PerformanceRegistryTest extends TestCase {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceRegistryTest.class);

    @Test
    public void testRegistry() {
        // 读取参数
        if (PerformanceUtils.getProperty("server", null) == null) {
            logger.warn("Please set -Dserver=127.0.0.1:9090");
            return;
        }
        final int base = PerformanceUtils.getIntProperty("base", 0);
        final int concurrent = PerformanceUtils.getIntProperty("concurrent", 100);
        int r = PerformanceUtils.getIntProperty("runs", 1000);
        final int runs = r > 0 ? r : Integer.MAX_VALUE;
        final Registry registry = ExtensionLoader.getExtensionLoader(RegistryFactory.class).getAdaptiveExtension().getRegistry(URL.valueOf("remote://admin:hello1234@" + PerformanceUtils.getProperty("server", "10.20.153.28:9090")));
        for (int i = 0; i < concurrent; i++) {
            final int t = i;
            new Thread(new Runnable() {
                public void run() {
                    for (int j = 0; j < runs; j++) {
                        registry.register(URL.valueOf("remote://" + NetUtils.getLocalHost() + ":8080/demoService" + t + "_" + j + "?version=1.0.0&application=demo&dubbo=2.0&interface=" + "com.alibaba.dubbo.demo.DemoService" + (base + t) + "_" + (base + j)));
                    }
                }
            }).start();
        }
        synchronized (PerformanceRegistryTest.class) {
            while (true) {
                try {
                    PerformanceRegistryTest.class.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

}