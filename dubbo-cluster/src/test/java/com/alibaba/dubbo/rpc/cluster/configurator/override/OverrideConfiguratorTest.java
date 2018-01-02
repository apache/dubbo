/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.cluster.configurator.override;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.rpc.cluster.configurator.absent.AbsentConfigurator;

import junit.framework.Assert;
import org.junit.Test;

/**
 * OverrideConfiguratorTest
 *
 */
public class OverrideConfiguratorTest {

    @Test
    public void testOverride_Application() {
        OverrideConfigurator configurator = new OverrideConfigurator(URL.valueOf("override://foo@0.0.0.0/com.foo.BarService?timeout=200"));

        URL url = configurator.configure(URL.valueOf("dubbo://10.20.153.10:20880/com.foo.BarService?application=foo&side=consumer"));
        Assert.assertEquals("200", url.getParameter("timeout"));

        url = configurator.configure(URL.valueOf("dubbo://10.20.153.10:20880/com.foo.BarService?application=foo&timeout=1000&side=consumer"));
        Assert.assertEquals("200", url.getParameter("timeout"));

        url = configurator.configure(URL.valueOf("dubbo://10.20.153.11:20880/com.foo.BarService?application=bar&side=consumer"));
        Assert.assertNull(url.getParameter("timeout"));

        url = configurator.configure(URL.valueOf("dubbo://10.20.153.11:20880/com.foo.BarService?application=bar&timeout=1000&side=consumer"));
        Assert.assertEquals("1000", url.getParameter("timeout"));
    }

    @Test
    public void testOverride_Host() {
        OverrideConfigurator configurator = new OverrideConfigurator(URL.valueOf("override://" + NetUtils.getLocalHost() + "/com.foo.BarService?timeout=200"));

        URL url = configurator.configure(URL.valueOf("dubbo://10.20.153.10:20880/com.foo.BarService?application=foo&side=consumer"));
        Assert.assertEquals("200", url.getParameter("timeout"));

        url = configurator.configure(URL.valueOf("dubbo://10.20.153.10:20880/com.foo.BarService?application=foo&timeout=1000&side=consumer"));
        Assert.assertEquals("200", url.getParameter("timeout"));

        AbsentConfigurator configurator1 = new AbsentConfigurator(URL.valueOf("override://10.20.153.10/com.foo.BarService?timeout=200"));

        url = configurator1.configure(URL.valueOf("dubbo://10.20.153.10:20880/com.foo.BarService?application=bar&side=consumer"));
        Assert.assertNull(url.getParameter("timeout"));

        url = configurator1.configure(URL.valueOf("dubbo://10.20.153.10:20880/com.foo.BarService?application=bar&timeout=1000&side=consumer"));
        Assert.assertEquals("1000", url.getParameter("timeout"));
    }

}
