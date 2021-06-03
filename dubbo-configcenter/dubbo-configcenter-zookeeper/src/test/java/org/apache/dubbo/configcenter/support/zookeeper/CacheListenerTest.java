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
package org.apache.dubbo.configcenter.support.zookeeper;

import org.apache.dubbo.common.config.configcenter.ConfigChangeType;
import org.apache.dubbo.common.config.configcenter.ConfigurationListener;
import org.apache.dubbo.remoting.zookeeper.EventType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CacheListenerTest {
    @Test
    public void addAndRemoveTest() {
        CacheListener cacheListener = new CacheListener("/dubbo/config");

        ConfigurationListener listenerProvider1 = conf -> {
        };
        cacheListener.addListener("/dubbo/config/dubbo/provider", listenerProvider1);

        ConfigurationListener listenerProvider2 = conf -> {
        };
        cacheListener.addListener("/dubbo/config/dubbo/provider", listenerProvider2);


        Assertions.assertEquals(2, cacheListener.getConfigurationListeners("/dubbo/config/dubbo/provider").size());
        cacheListener.removeListener("/dubbo/config/dubbo/provider", listenerProvider1);
        Assertions.assertEquals(1, cacheListener.getConfigurationListeners("/dubbo/config/dubbo/provider").size());
        cacheListener.removeListener("/dubbo/config/dubbo/provider", listenerProvider2);
        Assertions.assertNull(cacheListener.getConfigurationListeners("/dubbo/provider"));
    }

    @Test
    public void dataChangedTest() {
        CacheListener cacheListener = new CacheListener("/dubbo/config");

        ConfigurationListener configurationListener = conf -> {
            Assertions.assertEquals(conf.getContent(), "hello world");
            Assertions.assertEquals(conf.getChangeType(), ConfigChangeType.MODIFIED);
            Assertions.assertEquals(conf.getKey(), "provider");
            Assertions.assertEquals(conf.getGroup(), "dubbo");
        };
        cacheListener.addListener("/dubbo/config/dubbo/provider", configurationListener);
        cacheListener.dataChanged("/dubbo/config/dubbo/provider", "hello world", EventType.NodeDataChanged);
    }
}
