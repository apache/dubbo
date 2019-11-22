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
package org.apache.dubbo.registry.zookeeper.util;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.client.ServiceInstance;

import org.apache.curator.framework.CuratorFramework;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * The enumeration for the parameters  of {@link CuratorFramework}
 *
 * @see CuratorFramework
 * @since 2.7.5
 */
public enum CuratorFrameworkParams {

    /**
     * The root path of Dubbo Service
     */
    ROOT_PATH("rootPath", "/services", value -> value),

    /**
     * The host of current {@link ServiceInstance service instance} that will be registered
     */
    INSTANCE_HOST("instanceHost", null, value -> value),

    /**
     * The port of current {@link ServiceInstance service instance} that will be registered
     */
    INSTANCE_PORT("instancePort", null, value -> value),

    /**
     * Initial amount of time to wait between retries
     */
    BASE_SLEEP_TIME("baseSleepTimeMs", 50, Integer::valueOf),

    /**
     * Max number of times to retry.
     */
    MAX_RETRIES("maxRetries", 10, Integer::valueOf),

    /**
     * Max time in ms to sleep on each retry.
     */
    MAX_SLEEP("maxSleepMs", 500, Integer::valueOf),

    /**
     * Wait time to block on connection to Zookeeper.
     */
    BLOCK_UNTIL_CONNECTED_WAIT("blockUntilConnectedWait", 10, Integer::valueOf),

    /**
     * The unit of time related to blocking on connection to Zookeeper.
     */
    BLOCK_UNTIL_CONNECTED_UNIT("blockUntilConnectedUnit", TimeUnit.SECONDS, TimeUnit::valueOf),

    ;

    private final String name;

    private final Object defaultValue;

    private final Function<String, Object> converter;

    <T> CuratorFrameworkParams(String name, T defaultValue, Function<String, T> converter) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.converter = (Function<String, Object>) converter;
    }

    /**
     * Get the parameter value from the specified {@link URL}
     *
     * @param url the Dubbo registry {@link URL}
     * @param <T> the type of value
     * @return the parameter value if present, or return <code>null</code>
     */
    public <T> T getParameterValue(URL url) {
        String param = url.getParameter(name);
        Object value = param != null ? converter.apply(param) : defaultValue;
        return (T) value;
    }
}

