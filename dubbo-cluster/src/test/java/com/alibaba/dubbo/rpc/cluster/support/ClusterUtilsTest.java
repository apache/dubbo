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
package com.alibaba.dubbo.rpc.cluster.support;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class ClusterUtilsTest {

    @Test
    public void testMergeUrl() throws Exception {
        URL providerURL = URL.valueOf("dubbo://localhost:55555");
        providerURL = providerURL.setPath("path")
                .setUsername("username")
                .setPassword("password");

        providerURL = providerURL.addParameter(Constants.GROUP_KEY, "dubbo")
                .addParameter(Constants.VERSION_KEY, "1.2.3")
                .addParameter(Constants.DUBBO_VERSION_KEY, "2.3.7")
                .addParameter(Constants.THREADPOOL_KEY, "fixed")
                .addParameter(Constants.THREADS_KEY, Integer.MAX_VALUE)
                .addParameter(Constants.THREAD_NAME_KEY, "test")
                .addParameter(Constants.CORE_THREADS_KEY, Integer.MAX_VALUE)
                .addParameter(Constants.QUEUES_KEY, Integer.MAX_VALUE)
                .addParameter(Constants.ALIVE_KEY, Integer.MAX_VALUE)
                .addParameter(Constants.DEFAULT_KEY_PREFIX + Constants.THREADS_KEY, Integer.MAX_VALUE)
                .addParameter(Constants.DEFAULT_KEY_PREFIX + Constants.THREADPOOL_KEY, "fixed")
                .addParameter(Constants.DEFAULT_KEY_PREFIX + Constants.CORE_THREADS_KEY, Integer.MAX_VALUE)
                .addParameter(Constants.DEFAULT_KEY_PREFIX + Constants.QUEUES_KEY, Integer.MAX_VALUE)
                .addParameter(Constants.DEFAULT_KEY_PREFIX + Constants.ALIVE_KEY, Integer.MAX_VALUE)
                .addParameter(Constants.DEFAULT_KEY_PREFIX + Constants.THREAD_NAME_KEY, "test");

        URL consumerURL = URL.valueOf("dubbo://localhost:55555");
        consumerURL = consumerURL.addParameter(Constants.PID_KEY, "1234");
        consumerURL = consumerURL.addParameter(Constants.THREADPOOL_KEY, "foo");

        URL url = ClusterUtils.mergeUrl(providerURL, consumerURL.getParameters());

        Assert.assertFalse(url.hasParameter(Constants.THREADS_KEY));
        Assert.assertFalse(url.hasParameter(Constants.DEFAULT_KEY_PREFIX + Constants.THREADS_KEY));

        Assert.assertFalse(url.hasParameter(Constants.DEFAULT_KEY_PREFIX + Constants.THREADPOOL_KEY));

        Assert.assertFalse(url.hasParameter(Constants.CORE_THREADS_KEY));
        Assert.assertFalse(url.hasParameter(Constants.DEFAULT_KEY_PREFIX + Constants.CORE_THREADS_KEY));

        Assert.assertFalse(url.hasParameter(Constants.QUEUES_KEY));
        Assert.assertFalse(url.hasParameter(Constants.DEFAULT_KEY_PREFIX + Constants.QUEUES_KEY));

        Assert.assertFalse(url.hasParameter(Constants.ALIVE_KEY));
        Assert.assertFalse(url.hasParameter(Constants.DEFAULT_KEY_PREFIX + Constants.ALIVE_KEY));

        Assert.assertFalse(url.hasParameter(Constants.THREAD_NAME_KEY));
        Assert.assertFalse(url.hasParameter(Constants.DEFAULT_KEY_PREFIX + Constants.THREAD_NAME_KEY));

        Assert.assertEquals(url.getPath(), "path");
        Assert.assertEquals(url.getUsername(), "username");
        Assert.assertEquals(url.getPassword(), "password");
        Assert.assertEquals(url.getParameter(Constants.PID_KEY), "1234");
        Assert.assertEquals(url.getParameter(Constants.THREADPOOL_KEY), "foo");
    }

}
