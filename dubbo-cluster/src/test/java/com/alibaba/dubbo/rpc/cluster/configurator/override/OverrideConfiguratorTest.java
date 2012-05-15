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
package com.alibaba.dubbo.rpc.cluster.configurator.override;

import junit.framework.Assert;

import org.junit.Test;

import com.alibaba.dubbo.common.URL;

/**
 * OverrideConfiguratorTest
 * 
 * @author william.liangf
 */
public class OverrideConfiguratorTest {

    @Test
    public void testOverride_Application(){
        OverrideConfigurator configurator = new OverrideConfigurator(URL.valueOf("override://foo@0.0.0.0/com.foo.BarService?timeout=200"));
        
        URL url = configurator.configure(URL.valueOf("dubbo://10.20.153.10:20880/com.foo.BarService?application=foo"));
        Assert.assertEquals("200", url.getParameter("timeout"));
        
        url = configurator.configure(URL.valueOf("dubbo://10.20.153.10:20880/com.foo.BarService?application=foo&timeout=1000"));
        Assert.assertEquals("200", url.getParameter("timeout"));
        
        url = configurator.configure(URL.valueOf("dubbo://10.20.153.11:20880/com.foo.BarService?application=bar"));
        Assert.assertNull(url.getParameter("timeout"));
        
        url = configurator.configure(URL.valueOf("dubbo://10.20.153.11:20880/com.foo.BarService?application=bar&timeout=1000"));
        Assert.assertEquals("1000", url.getParameter("timeout"));
    }

    @Test
    public void testOverride_Host(){
        OverrideConfigurator configurator = new OverrideConfigurator(URL.valueOf("override://10.20.153.10/com.foo.BarService?timeout=200"));
        
        URL url = configurator.configure(URL.valueOf("dubbo://10.20.153.10:20880/com.foo.BarService?application=foo"));
        Assert.assertEquals("200", url.getParameter("timeout"));
        
        url = configurator.configure(URL.valueOf("dubbo://10.20.153.10:20880/com.foo.BarService?application=foo&timeout=1000"));
        Assert.assertEquals("200", url.getParameter("timeout"));
        
        url = configurator.configure(URL.valueOf("dubbo://10.20.153.11:20880/com.foo.BarService?application=bar"));
        Assert.assertNull(url.getParameter("timeout"));
        
        url = configurator.configure(URL.valueOf("dubbo://10.20.153.11:20880/com.foo.BarService?application=bar&timeout=1000"));
        Assert.assertEquals("1000", url.getParameter("timeout"));
    }

}
