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
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.ALIVE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.CORE_THREADS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_KEY_PREFIX;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_PROTOCOL;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_VERSION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PID_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.QUEUES_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REFERENCE_FILTER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TAG_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREADPOOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREADS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREAD_NAME_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.URL_MERGE_PROCESSOR_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;


public class ClusterUtilsTest {

    private ClusterUtils clusterUtils;

    @BeforeEach
    public void setup() {
        clusterUtils = new ClusterUtils();
        clusterUtils.setApplicationModel(ApplicationModel.defaultModel());
    }

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
            .addParameter(TAG_KEY, "TTT")
            .build();

        // Verify default ProviderURLMergeProcessor
        URL consumerURL = new URLBuilder(DUBBO_PROTOCOL, "localhost", 55555)
            .addParameter(PID_KEY, "1234")
            .addParameter(THREADPOOL_KEY, "foo")
            .addParameter(APPLICATION_KEY, "consumer")
            .addParameter(REFERENCE_FILTER_KEY, "filter3")
            .addParameter(TAG_KEY, "UUU")
            .build();

        URL url = clusterUtils.mergeUrl(providerURL, consumerURL.getParameters());

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

        Assertions.assertEquals("path", url.getPath());
        Assertions.assertEquals("username", url.getUsername());
        Assertions.assertEquals("password", url.getPassword());
        Assertions.assertEquals("1234", url.getParameter(PID_KEY));
        Assertions.assertEquals("foo", url.getParameter(THREADPOOL_KEY));
        Assertions.assertEquals("consumer", url.getApplication());
        Assertions.assertEquals("provider", url.getRemoteApplication());
        Assertions.assertEquals("filter1,filter2,filter3", url.getParameter(REFERENCE_FILTER_KEY));

        Assertions.assertEquals("TTT", url.getParameter(TAG_KEY));

        // Verify custom ProviderURLMergeProcessor
        URL consumerUrlForTag = new URLBuilder(DUBBO_PROTOCOL, "localhost", 55555)
            .addParameter(PID_KEY, "1234")
            .addParameter(THREADPOOL_KEY, "foo")
            .addParameter(APPLICATION_KEY, "consumer")
            .addParameter(REFERENCE_FILTER_KEY, "filter3")
            .addParameter(TAG_KEY, "UUU")
            .addParameter(URL_MERGE_PROCESSOR_KEY, "tag")
            .build();

        URL urlForTag = clusterUtils.mergeUrl(providerURL, consumerUrlForTag.getParameters());
        Assertions.assertEquals("UUU", urlForTag.getParameter(TAG_KEY));
    }

    @Test
    public void testMergeLocalParams() {

        // Verify default ProviderURLMergeProcessor
        URL consumerURL = new URLBuilder(DUBBO_PROTOCOL, "localhost", 55555)
            .addParameter(PID_KEY, "1234")
            .addParameter(THREADPOOL_KEY, "foo")
            .addParameter(APPLICATION_KEY, "consumer")
            .addParameter(REFERENCE_FILTER_KEY, "filter3")
            .addParameter(TAG_KEY, "UUU")
            .build();

        Map<String,String> params = clusterUtils.mergeLocalParams(consumerURL.getParameters());

        Assertions.assertEquals("1234", params.get(PID_KEY));
        Assertions.assertEquals("foo", params.get(THREADPOOL_KEY));
        Assertions.assertEquals("consumer", params.get(APPLICATION_KEY));
        Assertions.assertEquals("filter3", params.get(REFERENCE_FILTER_KEY));
        Assertions.assertEquals("UUU", params.get(TAG_KEY));

        // Verify custom ProviderURLMergeProcessor
        URL consumerUrlForTag = new URLBuilder(DUBBO_PROTOCOL, "localhost", 55555)
            .addParameter(PID_KEY, "1234")
            .addParameter(THREADPOOL_KEY, "foo")
            .addParameter(APPLICATION_KEY, "consumer")
            .addParameter(REFERENCE_FILTER_KEY, "filter3")
            .addParameter(TAG_KEY, "UUU")
            .addParameter(URL_MERGE_PROCESSOR_KEY, "tag")
            .build();

        Map<String,String> paramsForTag = clusterUtils.mergeLocalParams(consumerUrlForTag.getParameters());

        Assertions.assertEquals("1234", paramsForTag.get(PID_KEY));
        Assertions.assertEquals("foo", paramsForTag.get(THREADPOOL_KEY));
        Assertions.assertEquals("consumer", paramsForTag.get(APPLICATION_KEY));
        Assertions.assertEquals("filter3", paramsForTag.get(REFERENCE_FILTER_KEY));
        Assertions.assertNull(paramsForTag.get(TAG_KEY));
    }
}
