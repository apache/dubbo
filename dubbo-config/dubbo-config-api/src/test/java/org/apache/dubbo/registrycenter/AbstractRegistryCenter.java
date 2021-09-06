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
package org.apache.dubbo.registrycenter;

import org.apache.curator.test.InstanceSpec;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * The abstraction of {@link RegistryCenter} implements the basic methods.
 */
abstract class AbstractRegistryCenter implements RegistryCenter {

    /**
     * The default data directory is null
     */
    private static final File DEFAULT_DATA_DIRECTORY = null;

    /**
     * The default election port is -1.
     */
    private static final int DEFAULT_ELECTION_PORT = -1;

    /**
     * The default quorum port is -1.
     */
    private static final int DEFAULT_QUORUM_PORT = -1;

    /**
     * The default value is true.
     */
    private static final boolean DEFAULT_DELETE_DATA_DIRECTORY_ON_CLOSE = true;

    /**
     * The default service id is -1.
     */
    private static final int DEFAULT_SERVER_ID = -1;

    /**
     * The default tick time is 5000
     */
    private static final int DEFAULT_TICK_TIME = 5 * 1000;

    /**
     * The default value is -1.
     */
    private static final int DEFAULT_MAX_CLIENT_CNXNS = 200;

    /**
     * The minimum session timeout.
     */
    private static final int DEFAULT_MINIMUM_SESSION_TIMEOUT = DEFAULT_TICK_TIME * 2;

    /**
     * The maximum session timeout.
     */
    private static final int DEFAULT_MAXIMUM_SESSION_TIMEOUT = 60 * 1000;

    /**
     * The default customer properties.
     */
    private static final Map<String, Object> DEFAULT_CUSTOM_PROPERTIES = new HashMap<>(2);

    /**
     * The default hostname.
     */
    private static final String DEFAULT_HOSTNAME = "127.0.0.1";

    static {
        DEFAULT_CUSTOM_PROPERTIES.put("minSessionTimeout", DEFAULT_MINIMUM_SESSION_TIMEOUT);
        DEFAULT_CUSTOM_PROPERTIES.put("maxSessionTimeout", DEFAULT_MAXIMUM_SESSION_TIMEOUT);
    }

    /**
     * Create an {@link InstanceSpec} instance to initialize {@link org.apache.curator.test.TestingServer}
     *
     * @param port the zookeeper server's port.
     */
    protected InstanceSpec createInstanceSpec(int port) {
        return new InstanceSpec(DEFAULT_DATA_DIRECTORY,
            port,
            DEFAULT_ELECTION_PORT,
            DEFAULT_QUORUM_PORT,
            DEFAULT_DELETE_DATA_DIRECTORY_ON_CLOSE,
            DEFAULT_SERVER_ID,
            DEFAULT_TICK_TIME,
            DEFAULT_MAX_CLIENT_CNXNS,
            DEFAULT_CUSTOM_PROPERTIES,
            DEFAULT_HOSTNAME);
    }
}
