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
package org.apache.dubbo.test.check.registrycenter.config;

import org.apache.dubbo.test.check.registrycenter.Config;

/**
 * Define the zookeeper global config for registry center.
 */
public class ZookeeperRegistryCenterConfig {

    /**
     * Define the {@link Config} instance.
     */
    private static final Config CONFIG = new ZookeeperConfig();

    /**
     * Returns the connection address in single registry center.
     */
    public static String getConnectionAddress() {
        return CONFIG.getConnectionAddress();
    }

    /**
     * Returns the first connection address in multiple registry centers.
     */
    public static String getConnectionAddress1() {
        return CONFIG.getConnectionAddress1();
    }

    /**
     * Returns the second connection address in multiple registry centers.
     */
    public static String getConnectionAddress2() {
        return CONFIG.getConnectionAddress2();
    }

    /**
     * Returns the default connection address key in single registry center.
     * <h3>How to use</h3>
     * <pre>
     * System.getProperty({@link ZookeeperRegistryCenterConfig#getConnectionAddressKey()})
     * </pre>
     */
    public static String getConnectionAddressKey() {
        return CONFIG.getConnectionAddressKey();
    }

    /**
     * Returns the first connection address key in multiple registry center.
     * <h3>How to use</h3>
     * <pre>
     * System.getProperty({@link ZookeeperRegistryCenterConfig#getConnectionAddressKey1()})
     * </pre>
     */
    public static String getConnectionAddressKey1() {
        return CONFIG.getConnectionAddressKey1();
    }

    /**
     * Returns the second connection address key in multiple registry center.
     * <h3>How to use</h3>
     * <pre>
     * System.getProperty({@link ZookeeperRegistryCenterConfig#getConnectionAddressKey2()})
     * </pre>
     */
    public static String getConnectionAddressKey2() {
        return CONFIG.getConnectionAddressKey2();
    }
}
