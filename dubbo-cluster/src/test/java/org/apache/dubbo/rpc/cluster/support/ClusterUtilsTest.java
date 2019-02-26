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
package org.apache.dubbo.rpc.cluster.support;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;

import org.apache.dubbo.common.URLBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ClusterUtilsTest {

    @Test
    public void testMergeUrl() throws Exception {
        URL providerURL = URL.valueOf("dubbo://localhost:55555");
        providerURL = providerURL.setPath("path")
                .setUsername("username")
                .setPassword("password");

        providerURL = URLBuilder.from(providerURL)
                .addParameter(Constants.GROUP_KEY, "dubbo")
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
                .addParameter(Constants.DEFAULT_KEY_PREFIX + Constants.THREAD_NAME_KEY, "test")
                .build();

        URL consumerURL = new URLBuilder(Constants.DUBBO_PROTOCOL, "localhost", 55555)
                .addParameter(Constants.PID_KEY, "1234")
                .addParameter(Constants.THREADPOOL_KEY, "foo")
                .build();

        URL url = ClusterUtils.mergeUrl(providerURL, consumerURL.getParameters());

        Assertions.assertFalse(url.hasParameter(Constants.THREADS_KEY));
        Assertions.assertFalse(url.hasParameter(Constants.DEFAULT_KEY_PREFIX + Constants.THREADS_KEY));

        Assertions.assertFalse(url.hasParameter(Constants.DEFAULT_KEY_PREFIX + Constants.THREADPOOL_KEY));

        Assertions.assertFalse(url.hasParameter(Constants.CORE_THREADS_KEY));
        Assertions.assertFalse(url.hasParameter(Constants.DEFAULT_KEY_PREFIX + Constants.CORE_THREADS_KEY));

        Assertions.assertFalse(url.hasParameter(Constants.QUEUES_KEY));
        Assertions.assertFalse(url.hasParameter(Constants.DEFAULT_KEY_PREFIX + Constants.QUEUES_KEY));

        Assertions.assertFalse(url.hasParameter(Constants.ALIVE_KEY));
        Assertions.assertFalse(url.hasParameter(Constants.DEFAULT_KEY_PREFIX + Constants.ALIVE_KEY));

        Assertions.assertFalse(url.hasParameter(Constants.THREAD_NAME_KEY));
        Assertions.assertFalse(url.hasParameter(Constants.DEFAULT_KEY_PREFIX + Constants.THREAD_NAME_KEY));

        Assertions.assertEquals(url.getPath(), "path");
        Assertions.assertEquals(url.getUsername(), "username");
        Assertions.assertEquals(url.getPassword(), "password");
        Assertions.assertEquals(url.getParameter(Constants.PID_KEY), "1234");
        Assertions.assertEquals(url.getParameter(Constants.THREADPOOL_KEY), "foo");
    }

}
