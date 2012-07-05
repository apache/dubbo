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
package com.alibaba.dubbo.monitor.support;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.monitor.Monitor;
import com.alibaba.dubbo.monitor.MonitorFactory;

/**
 * AbstractMonitorFactoryTest
 * 
 * @author william.liangf
 */
public class AbstractMonitorFactoryTest {
    
    private MonitorFactory monitorFactory = new AbstractMonitorFactory() {
        
        protected Monitor createMonitor(final URL url) {
            return new Monitor() {

				public URL getUrl() {
					return url;
				}

				public boolean isAvailable() {
					return true;
				}

                public void destroy() {
                }
                
				public void collect(URL statistics) {
				}

				public List<URL> lookup(URL query) {
					return null;
				}
                
            };
        }
    };
    
    @Test
    public void testMonitorFactoryCache() throws Exception {
        URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostAddress() + ":2233");
        Monitor monitor1 = monitorFactory.getMonitor(url);
        Monitor monitor2 = monitorFactory.getMonitor(url);
        Assert.assertEquals(monitor1, monitor2);
    }
    
    @Test
    public void testMonitorFactoryIpCache() throws Exception {
        Monitor monitor1 = monitorFactory.getMonitor(URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":2233"));
        Monitor monitor2 = monitorFactory.getMonitor(URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostAddress() + ":2233"));
        Assert.assertEquals(monitor1, monitor2);
    }

    @Test
    public void testMonitorFactoryGroupCache() throws Exception {
        Monitor monitor1 = monitorFactory.getMonitor(URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":2233?group=aaa"));
        Monitor monitor2 = monitorFactory.getMonitor(URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":2233?group=bbb"));
        Assert.assertNotSame(monitor1, monitor2);
    }

}