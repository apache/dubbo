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
import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.CLUSTER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.CORE_THREADS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_KEY_PREFIX;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_PROTOCOL;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METHODS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PID_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.QUEUES_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.RELEASE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREADPOOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREADS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREAD_NAME_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMESTAMP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.remoting.Constants.DUBBO_VERSION_KEY;
import static org.apache.dubbo.rpc.Constants.REFERENCE_FILTER_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.LOADBALANCE_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.TAG_KEY;

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
                .addParameter(APPLICATION_KEY, "provider")
                .addParameter(REFERENCE_FILTER_KEY, "filter1,filter2")
                .build();

        URL consumerURL = new URLBuilder(DUBBO_PROTOCOL, "localhost", 55555)
                .addParameter(PID_KEY, "1234")
                .addParameter(THREADPOOL_KEY, "foo")
                .addParameter(APPLICATION_KEY, "consumer")
                .addParameter(REFERENCE_FILTER_KEY, "filter3")
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
        Assertions.assertEquals(url.getParameter(APPLICATION_KEY), "consumer");
        Assertions.assertEquals(url.getParameter(REMOTE_APPLICATION_KEY), "provider");
        Assertions.assertEquals(url.getParameter(REFERENCE_FILTER_KEY), "filter1,filter2,filter3");
    }

    @Test
    public void testUseProviderParams() {
        // present in both local and remote, but uses remote value.
        URL localURL = URL.valueOf("dubbo://localhost:20880/DemoService?version=local&group=local&dubbo=local&release=local" +
                "&methods=local&tag=local&timestamp=local");
        URL remoteURL = URL.valueOf("dubbo://localhost:20880/DemoService?version=remote&group=remote&dubbo=remote&release=remote" +
                "&methods=remote&tag=remote&timestamp=remote");
        URL mergedUrl = ClusterUtils.mergeUrl(remoteURL, localURL.getParameters());

        Assertions.assertEquals(remoteURL.getParameter(VERSION_KEY), mergedUrl.getParameter(VERSION_KEY));
        Assertions.assertEquals(remoteURL.getParameter(GROUP_KEY), mergedUrl.getParameter(GROUP_KEY));
        Assertions.assertEquals(remoteURL.getParameter(DUBBO_VERSION_KEY), mergedUrl.getParameter(DUBBO_VERSION_KEY));
        Assertions.assertEquals(remoteURL.getParameter(RELEASE_KEY), mergedUrl.getParameter(RELEASE_KEY));
        Assertions.assertEquals(remoteURL.getParameter(METHODS_KEY), mergedUrl.getParameter(METHODS_KEY));
        Assertions.assertEquals(remoteURL.getParameter(TIMESTAMP_KEY), mergedUrl.getParameter(TIMESTAMP_KEY));
        Assertions.assertEquals(remoteURL.getParameter(TAG_KEY), mergedUrl.getParameter(TAG_KEY));

        // present in local url but not in remote url, parameters of remote url is empty
        localURL = URL.valueOf("dubbo://localhost:20880/DemoService?version=local&group=local&dubbo=local&release=local" +
                "&methods=local&tag=local&timestamp=local");
        remoteURL = URL.valueOf("dubbo://localhost:20880/DemoService");
        mergedUrl = ClusterUtils.mergeUrl(remoteURL, localURL.getParameters());

        Assertions.assertNull(mergedUrl.getParameter(VERSION_KEY));
        Assertions.assertNull(mergedUrl.getParameter(GROUP_KEY));
        Assertions.assertNull(mergedUrl.getParameter(DUBBO_VERSION_KEY));
        Assertions.assertNull(mergedUrl.getParameter(RELEASE_KEY));
        Assertions.assertNull(mergedUrl.getParameter(METHODS_KEY));
        Assertions.assertNull(mergedUrl.getParameter(TIMESTAMP_KEY));
        Assertions.assertNull(mergedUrl.getParameter(TAG_KEY));

        // present in local url but not in remote url
        localURL = URL.valueOf("dubbo://localhost:20880/DemoService?version=local&group=local&dubbo=local&release=local" +
                "&methods=local&tag=local&timestamp=local");
        remoteURL = URL.valueOf("dubbo://localhost:20880/DemoService?key=value");
        mergedUrl = ClusterUtils.mergeUrl(remoteURL, localURL.getParameters());

        Assertions.assertNull(mergedUrl.getParameter(VERSION_KEY));
        Assertions.assertNull(mergedUrl.getParameter(GROUP_KEY));
        Assertions.assertNull(mergedUrl.getParameter(DUBBO_VERSION_KEY));
        Assertions.assertNull(mergedUrl.getParameter(RELEASE_KEY));
        Assertions.assertNull(mergedUrl.getParameter(METHODS_KEY));
        Assertions.assertNull(mergedUrl.getParameter(TIMESTAMP_KEY));
        Assertions.assertNull(mergedUrl.getParameter(TAG_KEY));

        // present in both local and remote, uses local url params
        localURL = URL.valueOf("dubbo://localhost:20880/DemoService?loadbalance=local&timeout=1000&cluster=local");
        remoteURL = URL.valueOf("dubbo://localhost:20880/DemoService?loadbalance=remote&timeout=2000&cluster=remote");
        mergedUrl = ClusterUtils.mergeUrl(remoteURL, localURL.getParameters());

        Assertions.assertEquals(localURL.getParameter(CLUSTER_KEY), mergedUrl.getParameter(CLUSTER_KEY));
        Assertions.assertEquals(localURL.getParameter(TIMEOUT_KEY), mergedUrl.getParameter(TIMEOUT_KEY));
        Assertions.assertEquals(localURL.getParameter(LOADBALANCE_KEY), mergedUrl.getParameter(LOADBALANCE_KEY));
    }

}
