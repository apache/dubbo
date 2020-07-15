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
package org.apache.dubbo.metadata;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.metadata.store.InMemoryWritableMetadataService;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSortedSet;
import static org.apache.dubbo.common.URL.valueOf;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link InMemoryWritableMetadataService} Test
 *
 * @since 2.7.5
 */
public class InMemoryWritableMetadataServiceTest {

    private WritableMetadataService metadataService = new InMemoryWritableMetadataService();

    private static final String TEST_SERVICE = "org.apache.dubbo.test.TestService";

    private static final URL BASE_URL = valueOf("dubbo://127.0.0.1:20880/" + TEST_SERVICE);
    private static final URL REST_BASE_URL = valueOf("rest://127.0.0.1:20880/" + TEST_SERVICE);
    private static final URL BASE_URL_GROUP = BASE_URL.addParameter(GROUP_KEY, "test");
    private static final URL BASE_URL_GROUP_AND_VERSION = BASE_URL_GROUP.addParameter(VERSION_KEY, "1.0.0");
    private static final URL BASE_URL_GROUP_AND_VERSION_AND_PROTOCOL = BASE_URL_GROUP_AND_VERSION.addParameter(PROTOCOL_KEY, "rest");

    @BeforeEach
    public void init() {
        ApplicationConfig applicationConfig = new ApplicationConfig("test");
        ApplicationModel.getConfigManager().setApplication(applicationConfig);
    }

    @AfterEach
    public void reset() {
        ApplicationModel.reset();
    }

    @Test
    public void testServiceName() {
        assertEquals("test", metadataService.serviceName());
    }

    @Test
    public void testVersion() {
        assertEquals("1.0.0", MetadataService.VERSION);
        assertEquals("1.0.0", metadataService.version());
    }

    @Test
    public void testGetExportedURLs() {

        assertTrue(metadataService.exportURL(BASE_URL));
        Set<String> exportedURLs = metadataService.getExportedURLs(TEST_SERVICE);
        assertEquals(1, exportedURLs.size());
        assertEquals(asSortedSet(BASE_URL.toFullString()), exportedURLs);
        assertTrue(metadataService.unexportURL(BASE_URL));

        assertTrue(metadataService.exportURL(BASE_URL));
        assertFalse(metadataService.exportURL(BASE_URL));

        assertTrue(metadataService.exportURL(BASE_URL_GROUP));
        assertTrue(metadataService.exportURL(BASE_URL_GROUP_AND_VERSION));

        exportedURLs = metadataService.getExportedURLs(TEST_SERVICE);
        assertEquals(asSortedSet(BASE_URL.toFullString()), exportedURLs);
        assertEquals(asSortedSet(
                BASE_URL.toFullString(),
                BASE_URL_GROUP.toFullString(),
                BASE_URL_GROUP_AND_VERSION.toFullString()), metadataService.getExportedURLs());

        assertTrue(metadataService.exportURL(REST_BASE_URL));
        exportedURLs = metadataService.getExportedURLs(TEST_SERVICE);
        assertEquals(asSortedSet(BASE_URL.toFullString(), REST_BASE_URL.toFullString()), exportedURLs);

        metadataService.exportURL(BASE_URL_GROUP_AND_VERSION_AND_PROTOCOL);

        exportedURLs = metadataService.getExportedURLs(TEST_SERVICE, "test", "1.0.0", "rest");

        assertEquals(asSortedSet(BASE_URL_GROUP_AND_VERSION_AND_PROTOCOL.toFullString()), exportedURLs);
    }

    @Test
    public void testGetSubscribedURLs() {
        assertTrue(metadataService.subscribeURL(BASE_URL));
        assertFalse(metadataService.subscribeURL(BASE_URL));

        assertTrue(metadataService.subscribeURL(BASE_URL_GROUP));
        assertTrue(metadataService.subscribeURL(BASE_URL_GROUP_AND_VERSION));
        assertTrue(metadataService.subscribeURL(REST_BASE_URL));

        Set<String> subscribedURLs = metadataService.getSubscribedURLs();
        assertEquals(4, subscribedURLs.size());
        assertEquals(asSortedSet(
                BASE_URL.toFullString(),
                REST_BASE_URL.toFullString(),
                BASE_URL_GROUP.toFullString(),
                BASE_URL_GROUP_AND_VERSION.toFullString()), subscribedURLs);

        assertTrue(metadataService.unsubscribeURL(REST_BASE_URL));
        subscribedURLs = metadataService.getSubscribedURLs();
        assertEquals(3, subscribedURLs.size());
        assertEquals(asSortedSet(
                BASE_URL.toFullString(),
                BASE_URL_GROUP.toFullString(),
                BASE_URL_GROUP_AND_VERSION.toFullString()), subscribedURLs);

        assertTrue(metadataService.unsubscribeURL(BASE_URL_GROUP));
        subscribedURLs = metadataService.getSubscribedURLs();
        assertEquals(2, subscribedURLs.size());
        assertEquals(asSortedSet(
                BASE_URL.toFullString(),
                BASE_URL_GROUP_AND_VERSION.toFullString()), subscribedURLs);

        assertTrue(metadataService.unsubscribeURL(BASE_URL_GROUP_AND_VERSION));
        subscribedURLs = metadataService.getSubscribedURLs();
        assertEquals(1, subscribedURLs.size());
        assertEquals(asSortedSet(
                BASE_URL.toFullString()), subscribedURLs);
    }

    private static <T extends Comparable<T>> SortedSet<T> asSortedSet(T... values) {
        return unmodifiableSortedSet(new TreeSet<>(asList(values)));
    }

}