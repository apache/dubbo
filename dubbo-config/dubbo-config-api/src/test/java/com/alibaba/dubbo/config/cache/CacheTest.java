/*
 * Copyright 1999-2012 Alibaba Group.
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
package com.alibaba.dubbo.config.cache;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ProtocolConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.ServiceConfig;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

/**
 * CacheTest
 *
 * @author william.liangf
 */
public class CacheTest extends TestCase {

    @Test
    public void testCache() throws Exception {
        ServiceConfig<CacheService> service = new ServiceConfig<CacheService>();
        service.setApplication(new ApplicationConfig("cache-provider"));
        service.setRegistry(new RegistryConfig("N/A"));
        service.setProtocol(new ProtocolConfig("dubbo", 29582));
        service.setInterface(CacheService.class.getName());
        service.setRef(new CacheServiceImpl());
        service.export();
        try {
            ReferenceConfig<CacheService> reference = new ReferenceConfig<CacheService>();
            reference.setApplication(new ApplicationConfig("cache-consumer"));
            reference.setInterface(CacheService.class);
            reference.setUrl("dubbo://127.0.0.1:29582?scope=remote&cache=true");
            CacheService cacheService = reference.get();
            try {
                // 测试缓存生效，多次调用返回同样的结果。(服务器端自增长返回值)
                String fix = null;
                for (int i = 0; i < 3; i++) {
                    String result = cacheService.findCache("0");
                    Assert.assertTrue(fix == null || fix.equals(result));
                    fix = result;
                    Thread.sleep(100);
                }

                // LRU的缺省cache.size为1000，执行1001次，应有溢出
                for (int n = 0; n < 1001; n++) {
                    String pre = null;
                    for (int i = 0; i < 10; i++) {
                        String result = cacheService.findCache(String.valueOf(n));
                        Assert.assertTrue(pre == null || pre.equals(result));
                        pre = result;
                    }
                }

                // 测试LRU有移除最开始的一个缓存项
                String result = cacheService.findCache("0");
                Assert.assertFalse(fix == null || fix.equals(result));
            } finally {
                reference.destroy();
            }
        } finally {
            service.unexport();
        }
    }

}
