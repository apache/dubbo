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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.CommonConstants.ALIVE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.CORE_THREADS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_KEY_PREFIX;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PID_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.QUEUES_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREADPOOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREADS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREAD_NAME_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.constants.ConfigConstants.DUBBO_PROTOCOL;
import static org.apache.dubbo.common.constants.RpcConstants.DUBBO_VERSION_KEY;

public class ClusterUtilsTest {

    @Test
    public void testMergeUrl() throws Exception {
        URL providerURL = URL.valueOf("dubbo://localhost:55555");
        providerURL = providerURL.setPath("path")
                .setUsername("username")
                .setPassword("password");

        providerURL = URLBuilder.from(providerURL)
                .addParameter(GROUP_KEY, "dubbo")
                .addParameter(VERSION_KEY, "1.2.3")
                .addParameter(DUBBO_VERSION_KEY, "2.3.7")
                .addParameter(THREADPOOL_KEY, "fixed")
                .addParameter(THREADS_KEY, Integer.MAX_VALUE)
                .addParameter(THREAD_NAME_KEY, "test")
                .addParameter(CORE_THREADS_KEY, Integer.MAX_VALUE)
                .addParameter(QUEUES_KEY, Integer.MAX_VALUE)
                .addParameter(ALIVE_KEY, Integer.MAX_VALUE)
                .addParameter(DEFAULT_KEY_PREFIX + THREADS_KEY, Integer.MAX_VALUE)
                .addParameter(DEFAULT_KEY_PREFIX + THREADPOOL_KEY, "fixed")
                .addParameter(DEFAULT_KEY_PREFIX + CORE_THREADS_KEY, Integer.MAX_VALUE)
                .addParameter(DEFAULT_KEY_PREFIX + QUEUES_KEY, Integer.MAX_VALUE)
                .addParameter(DEFAULT_KEY_PREFIX + ALIVE_KEY, Integer.MAX_VALUE)
                .addParameter(DEFAULT_KEY_PREFIX + THREAD_NAME_KEY, "test")
                .build();

        URL consumerURL = new URLBuilder(DUBBO_PROTOCOL, "localhost", 55555)
                .addParameter(PID_KEY, "1234")
                .addParameter(THREADPOOL_KEY, "foo")
                .build();

        URL url = ClusterUtils.mergeUrl(providerURL, consumerURL.getParameters());

        Assertions.assertFalse(url.hasParameter(THREADS_KEY));
        Assertions.assertFalse(url.hasParameter(DEFAULT_KEY_PREFIX + THREADS_KEY));

        Assertions.assertFalse(url.hasParameter(DEFAULT_KEY_PREFIX + THREADPOOL_KEY));

        Assertions.assertFalse(url.hasParameter(CORE_THREADS_KEY));
        Assertions.assertFalse(url.hasParameter(DEFAULT_KEY_PREFIX + CORE_THREADS_KEY));

        Assertions.assertFalse(url.hasParameter(QUEUES_KEY));
        Assertions.assertFalse(url.hasParameter(DEFAULT_KEY_PREFIX + QUEUES_KEY));

        Assertions.assertFalse(url.hasParameter(ALIVE_KEY));
        Assertions.assertFalse(url.hasParameter(DEFAULT_KEY_PREFIX + ALIVE_KEY));

        Assertions.assertFalse(url.hasParameter(THREAD_NAME_KEY));
        Assertions.assertFalse(url.hasParameter(DEFAULT_KEY_PREFIX + THREAD_NAME_KEY));

        Assertions.assertEquals(url.getPath(), "path");
        Assertions.assertEquals(url.getUsername(), "username");
        Assertions.assertEquals(url.getPassword(), "password");
        Assertions.assertEquals(url.getParameter(PID_KEY), "1234");
        Assertions.assertEquals(url.getParameter(THREADPOOL_KEY), "foo");
    }

}
