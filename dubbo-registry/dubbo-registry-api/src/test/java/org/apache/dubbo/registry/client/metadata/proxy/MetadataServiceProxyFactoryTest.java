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
package org.apache.dubbo.registry.client.metadata.proxy;

import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.CommonConstants.COMPOSITE_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.registry.client.metadata.proxy.MetadataServiceProxyFactory.getDefaultExtension;
import static org.apache.dubbo.registry.client.metadata.proxy.MetadataServiceProxyFactory.getExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link MetadataServiceProxyFactory} Test-Cases
 *
 * @since 2.7.8
 */
public class MetadataServiceProxyFactoryTest {

    @Test
    public void testExtension() {
        MetadataServiceProxyFactory defaultFactory = getDefaultExtension();
        MetadataServiceProxyFactory factory = getExtension(DEFAULT_METADATA_STORAGE_TYPE);
        assertEquals(defaultFactory, factory);

        assertEquals(MyMetadataServiceProxyFactory.class, factory.getClass());

        factory = getExtension(REMOTE_METADATA_STORAGE_TYPE);
        assertEquals(RemoteMetadataServiceProxyFactory.class, factory.getClass());

        factory = getExtension(COMPOSITE_METADATA_STORAGE_TYPE);
        assertEquals(CompositeMetadataServiceProxyFactory.class, factory.getClass());
    }
}
