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
package com.alibaba.dubbo.registry.support;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.NetUtils;

/**
 * AbstractRegistryServiceTest
 * 
 * @author william.liangf
 */
public class AbstractRegistryServiceTest {
    
    @Test
    public void testNotified() {
        AbstractRegistryService registry = new AbstractRegistryService() {};
        
        List<URL> providers = new ArrayList<URL>();
        providers.add(URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":20880/com.alibaba.demo.DemoService"));
        registry.addNotified("com.alibaba.demo.DemoService", providers);
        Assert.assertEquals(1, registry.getNotified().size());
        Assert.assertEquals(1, registry.getNotified().get("com.alibaba.demo.DemoService").size());
        Assert.assertEquals("dubbo://" + NetUtils.getLocalHost() + ":20880/com.alibaba.demo.DemoService", registry.getNotified().get("com.alibaba.demo.DemoService").get(0).toFullString());
        Assert.assertEquals(1, registry.getNotified("com.alibaba.demo.DemoService").size());
        Assert.assertEquals("dubbo://" + NetUtils.getLocalHost() + ":20880/com.alibaba.demo.DemoService", registry.getNotified("com.alibaba.demo.DemoService").get(0).toFullString());
        
        providers = new ArrayList<URL>();
        providers.add(URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":20881/com.alibaba.demo.DemoService"));
        registry.addNotified("com.alibaba.demo.DemoService", providers);
        Assert.assertEquals(1, registry.getNotified().size());
        Assert.assertEquals(1, registry.getNotified().get("com.alibaba.demo.DemoService").size());
        Assert.assertEquals("dubbo://" + NetUtils.getLocalHost() + ":20881/com.alibaba.demo.DemoService", registry.getNotified().get("com.alibaba.demo.DemoService").get(0).toFullString());
        Assert.assertEquals(1, registry.getNotified("com.alibaba.demo.DemoService").size());
        Assert.assertEquals("dubbo://" + NetUtils.getLocalHost() + ":20881/com.alibaba.demo.DemoService", registry.getNotified("com.alibaba.demo.DemoService").get(0).toFullString());
        
        List<URL> overrides = new ArrayList<URL>();
        overrides.add(URL.valueOf("override://" + NetUtils.getLocalHost() + ":20881/com.alibaba.demo.DemoService?timeout=2000"));
        registry.addNotified("com.alibaba.demo.DemoService", overrides);
        Assert.assertEquals(1, registry.getNotified().size());
        Assert.assertEquals(2, registry.getNotified().get("com.alibaba.demo.DemoService").size());
        Assert.assertEquals("dubbo://" + NetUtils.getLocalHost() + ":20881/com.alibaba.demo.DemoService", registry.getNotified().get("com.alibaba.demo.DemoService").get(0).toFullString());
        Assert.assertEquals("override://" + NetUtils.getLocalHost() + ":20881/com.alibaba.demo.DemoService?timeout=2000", registry.getNotified().get("com.alibaba.demo.DemoService").get(1).toFullString());
        Assert.assertEquals(2, registry.getNotified("com.alibaba.demo.DemoService").size());
        Assert.assertEquals("dubbo://" + NetUtils.getLocalHost() + ":20881/com.alibaba.demo.DemoService", registry.getNotified("com.alibaba.demo.DemoService").get(0).toFullString());
        Assert.assertEquals("override://" + NetUtils.getLocalHost() + ":20881/com.alibaba.demo.DemoService?timeout=2000", registry.getNotified("com.alibaba.demo.DemoService").get(1).toFullString());
        
        List<URL> routes = new ArrayList<URL>();
        routes.add(URL.valueOf("route://" + NetUtils.getLocalHost() + ":20881/com.alibaba.demo.DemoService?rule=aa"));
        registry.addNotified("com.alibaba.demo.DemoService", routes);
        Assert.assertEquals(1, registry.getNotified().size());
        Assert.assertEquals(3, registry.getNotified().get("com.alibaba.demo.DemoService").size());
        Assert.assertEquals("dubbo://" + NetUtils.getLocalHost() + ":20881/com.alibaba.demo.DemoService", registry.getNotified().get("com.alibaba.demo.DemoService").get(0).toFullString());
        Assert.assertEquals("override://" + NetUtils.getLocalHost() + ":20881/com.alibaba.demo.DemoService?timeout=2000", registry.getNotified().get("com.alibaba.demo.DemoService").get(1).toFullString());
        Assert.assertEquals("route://" + NetUtils.getLocalHost() + ":20881/com.alibaba.demo.DemoService?rule=aa", registry.getNotified().get("com.alibaba.demo.DemoService").get(2).toFullString());
        Assert.assertEquals(3, registry.getNotified("com.alibaba.demo.DemoService").size());
        Assert.assertEquals("dubbo://" + NetUtils.getLocalHost() + ":20881/com.alibaba.demo.DemoService", registry.getNotified("com.alibaba.demo.DemoService").get(0).toFullString());
        Assert.assertEquals("override://" + NetUtils.getLocalHost() + ":20881/com.alibaba.demo.DemoService?timeout=2000", registry.getNotified("com.alibaba.demo.DemoService").get(1).toFullString());
        Assert.assertEquals("route://" + NetUtils.getLocalHost() + ":20881/com.alibaba.demo.DemoService?rule=aa", registry.getNotified("com.alibaba.demo.DemoService").get(2).toFullString());
        
    }

}
