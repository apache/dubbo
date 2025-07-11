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

import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.test.check.registrycenter.Config;

/**
 * The zookeeper config in registry center.
 * Uses dynamic port allocation to avoid conflicts in parallel testing.
 */
public class ZookeeperConfig implements Config {

    /**
     * The system properties config key with zookeeper connection address.
     */
    private static final String ZOOKEEPER_CONNECTION_ADDRESS_KEY = "zookeeper.connection.address";

    /**
     * The system properties config key with first zookeeper connection address.
     */
    private static final String ZOOKEEPER_CONNECTION_ADDRESS_1_KEY = "zookeeper.connection.address.1";

    /**
     * The system properties config key with second zookeeper connection address.
     */
    private static final String ZOOKEEPER_CONNECTION_ADDRESS_2_KEY = "zookeeper.connection.address.2";

    /**
     * The connection address format for zookeeper.
     */
    private static final String CONNECTION_ADDRESS_FORMAT = "zookeeper://127.0.0.1:%d";

    /**
     * The default admin server ports of zookeeper.
     */
    private static final int[] DEFAULT_ADMIN_SERVER_PORTS = new int[] {18081, 18082};

    /**
     * The default version of zookeeper.
     */
    private static final String DEFAULT_ZOOKEEPER_VERSION = "3.6.0";

    /**
     * The default client ports of zookeeper.
     * Uses dynamic port allocation to avoid conflicts in parallel testing.
     */
    private static final int[] CLIENT_PORTS = initializePorts();

    /**
     * Initialize two different reserved ports for ZooKeeper instances.
     * Uses port reservation mechanism to avoid conflicts in parallel testing.
     */
    private static int[] initializePorts() {
        String identifier = "ZookeeperConfig_" + System.currentTimeMillis() + "_"
                + Thread.currentThread().getId();
        int[] ports = NetUtils.getReservedPortsForTest(identifier, 2);

        int port1 = ports[0];
        int port2 = ports[1];

        // Set system properties for tests that rely on them
        System.setProperty(ZOOKEEPER_CONNECTION_ADDRESS_KEY, String.format(CONNECTION_ADDRESS_FORMAT, port1));
        System.setProperty(ZOOKEEPER_CONNECTION_ADDRESS_1_KEY, String.format(CONNECTION_ADDRESS_FORMAT, port1));
        System.setProperty(ZOOKEEPER_CONNECTION_ADDRESS_2_KEY, String.format(CONNECTION_ADDRESS_FORMAT, port2));

        return new int[] {port1, port2};
    }

    @Override
    public String getConnectionAddress1() {
        return String.format(CONNECTION_ADDRESS_FORMAT, CLIENT_PORTS[0]);
    }

    @Override
    public String getConnectionAddress2() {
        return String.format(CONNECTION_ADDRESS_FORMAT, CLIENT_PORTS[1]);
    }

    @Override
    public String getConnectionAddressKey() {
        return ZOOKEEPER_CONNECTION_ADDRESS_KEY;
    }

    @Override
    public String getConnectionAddressKey1() {
        return ZOOKEEPER_CONNECTION_ADDRESS_1_KEY;
    }

    @Override
    public String getConnectionAddressKey2() {
        return ZOOKEEPER_CONNECTION_ADDRESS_2_KEY;
    }

    /**
     * Returns the zookeeper's version.
     */
    public String getVersion() {
        return DEFAULT_ZOOKEEPER_VERSION;
    }

    /**
     * Returns the client ports of zookeeper.
     */
    public int[] getClientPorts() {
        return CLIENT_PORTS;
    }

    /**
     * Returns the admin server ports of zookeeper.
     */
    public int[] getAdminServerPorts() {
        return DEFAULT_ADMIN_SERVER_PORTS;
    }
}
