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
package com.alibaba.dubbo.rpc.cluster.configurator.absent;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.NetUtils;

import org.junit.Assert;
import org.junit.Test;

/**
 * OverrideConfiguratorTest
 *
 */
public class AbsentConfiguratorTest {

    public static final String URL_CONSUMER = "dubbo://10.20.153.10:20880/com.foo.BarService?application=foo&side=consumer";
    public static final String URL_ONE = "dubbo://10.20.153.10:20880/com.foo.BarService?application=foo&timeout=1000&side=consumer";

    @Test
    public void testOverrideApplication() {
        AbsentConfigurator configurator = new AbsentConfigurator(com.alibaba.dubbo.common.URL.valueOf("override://foo@0.0.0.0/com.foo.BarService?timeout=200"));

        URL url = configurator.configure(com.alibaba.dubbo.common.URL.valueOf(URL_CONSUMER));
        Assert.assertEquals("200", url.getParameter("timeout"));

        url = configurator.configure(com.alibaba.dubbo.common.URL.valueOf(URL_ONE));
        Assert.assertEquals("1000", url.getParameter("timeout"));

        url = configurator.configure(com.alibaba.dubbo.common.URL.valueOf("dubbo://10.20.153.11:20880/com.foo.BarService?application=bar&side=consumer"));
        Assert.assertNull(url.getParameter("timeout"));

        url = configurator.configure(com.alibaba.dubbo.common.URL.valueOf("dubbo://10.20.153.11:20880/com.foo.BarService?application=bar&timeout=1000&side=consumer"));
        Assert.assertEquals("1000", url.getParameter("timeout"));
    }

    @Test
    public void testOverrideHost() {
        AbsentConfigurator configurator = new AbsentConfigurator(com.alibaba.dubbo.common.URL.valueOf("override://" + NetUtils.getLocalHost() + "/com.foo.BarService?timeout=200"));

        URL url = configurator.configure(com.alibaba.dubbo.common.URL.valueOf(URL_CONSUMER));
        Assert.assertEquals("200", url.getParameter("timeout"));

        url = configurator.configure(com.alibaba.dubbo.common.URL.valueOf(URL_ONE));
        Assert.assertEquals("1000", url.getParameter("timeout"));

        AbsentConfigurator configurator1 = new AbsentConfigurator(com.alibaba.dubbo.common.URL.valueOf("override://10.20.153.10/com.foo.BarService?timeout=200"));

        url = configurator1.configure(com.alibaba.dubbo.common.URL.valueOf("dubbo://10.20.153.10:20880/com.foo.BarService?application=bar&side=consumer"));
        Assert.assertNull(url.getParameter("timeout"));

        url = configurator1.configure(com.alibaba.dubbo.common.URL.valueOf("dubbo://10.20.153.10:20880/com.foo.BarService?application=bar&timeout=1000&side=consumer"));
        Assert.assertEquals("1000", url.getParameter("timeout"));
    }

}
