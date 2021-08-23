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
package org.apache.dubbo.config.spring.registrycenter;

/**
 * The default zookeeper multiple registry center.
 */
public class ZookeeperMultipleRegistryCenter extends ZookeeperRegistryCenter {

    /**
     * Initialize {@link ZookeeperMultipleRegistryCenter} instance.
     *
     * @param port1 the zookeeper server's port.
     * @param port2 the zookeeper server's port.
     */
    public ZookeeperMultipleRegistryCenter(int port1, int port2) {
        super(port1, port2);
    }

    /**
     * Initialize {@link ZookeeperMultipleRegistryCenter} instance.
     */
    public ZookeeperMultipleRegistryCenter() {
        this(DEFAULT_PORT1, DEFAULT_PORT2);
    }

    private static final int DEFAULT_PORT1 = 2181;
    private static final int DEFAULT_PORT2 = 2182;
}
