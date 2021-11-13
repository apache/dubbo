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
 * The zookeeper config in registry center.
 */
public class ZookeeperConfig implements Config {

    /**
     * The system properties config key with zookeeper port.
     */
    private static final String ZOOKEEPER_PORT_KEY = "zookeeper.port";

    /**
     * The system properties config key with first zookeeper port.
     */
    private static final String ZOOKEEPER_PORT_1_KEY = "zookeeper.port.1";

    /**
     * The system properties config key with second zookeeper port.
     */
    private static final String ZOOKEEPER_PORT_2_KEY = "zookeeper.port.2";

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
     * The default first client port of zookeeper.
     */
    public static final int DEFAULT_CLIENT_PORT_1 = 2181;

    /**
     * The default second client port of zookeeper.
     */
    public static final int DEFAULT_CLIENT_PORT_2 = 2182;

    /**
     * The default client ports of zookeeper.
     */
    private static final int[] CLIENT_PORTS = new int[2];

    /**
     * The default admin server ports of zookeeper.
     */
    private static final int[] DEFAULT_ADMIN_SERVER_PORTS = new int[]{18081, 18082};

    /**
     * The default version of zookeeper.
     */
    private static final String DEFAULT_ZOOKEEPER_VERSION = "3.6.0";

    /**
     * The format for zookeeper connection address.
     */
    private static final String CONNECTION_ADDRESS_FORMAT = "zookeeper://127.0.0.1:%d";

    // initialize the client ports of zookeeper.
    static {
        // There are two client ports

        // The priority of the one is that get it from system properties config
        // with the key of {@link #ZOOKEEPER_PORT_1_KEY} first, and then {@link #ZOOKEEPER_PORT_KEY},
        // finally use {@link #DEFAULT_CLIENT_PORT_1} as default port

        // The priority of the other is that get it from system properties config with the key of {@link #ZOOKEEPER_PORT_2_KEY} first,
        // and then use {@link #DEFAULT_CLIENT_PORT_2} as default port

        int port1 = DEFAULT_CLIENT_PORT_1;
        int port2 = DEFAULT_CLIENT_PORT_2;
        String portConfig1 = System.getProperty(ZOOKEEPER_PORT_1_KEY, System.getProperty(ZOOKEEPER_PORT_KEY));
        if (portConfig1 != null) {
            try {
                port1 = Integer.parseInt(portConfig1);
            } catch (NumberFormatException e) {
                port1 = DEFAULT_CLIENT_PORT_1;
            }
        }

        String portConfig2 = System.getProperty(ZOOKEEPER_PORT_2_KEY);
        if (portConfig2 != null) {
            try {
                port2 = Integer.parseInt(portConfig2);
            } catch (NumberFormatException e) {
                port2 = DEFAULT_CLIENT_PORT_2;
            }
        }

        if (port1 == port2) {
            throw new IllegalArgumentException(String.format("The client ports %d and %d of zookeeper cannot be same!", port1, port2));
        }

        CLIENT_PORTS[0] = port1;
        CLIENT_PORTS[1] = port2;

        // set system properties config
        System.setProperty(ZOOKEEPER_CONNECTION_ADDRESS_KEY, String.format(CONNECTION_ADDRESS_FORMAT, CLIENT_PORTS[0]));
        System.setProperty(ZOOKEEPER_CONNECTION_ADDRESS_1_KEY, String.format(CONNECTION_ADDRESS_FORMAT, CLIENT_PORTS[0]));
        System.setProperty(ZOOKEEPER_CONNECTION_ADDRESS_2_KEY, String.format(CONNECTION_ADDRESS_FORMAT, CLIENT_PORTS[1]));
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
