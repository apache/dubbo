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
package org.apache.dubbo.test.check.registrycenter;

/**
 * The mocked registry center.
 */
public class MockedRegistryCenter {

    /**
     * The first global zookeeper address
     */
    public static final String ZOOKEEPER_ADDRESS1 = "zookeeper://127.0.0.1:2181";

    /**
     * The second global zookeeper address
     */
    public static final String ZOOKEEPER_ADDRESS2 = "zookeeper://127.0.0.1:2182";

    /**
     * The default static registry center instance.
     */
    private static final RegistryCenter INSTANCE = new ZookeeperRegistryCenter();

    /**
     * Start the registry center.
     *
     * @throws Exception when an exception occurred
     */
    public static void startup() throws Exception {
        INSTANCE.startup();
    }

    /**
     * Reset the registry center.
     *
     * @throws Exception when an exception occurred
     */
    public static void reset() throws Exception {
        INSTANCE.reset();
    }

    /**
     * Stop the registry center.
     *
     * @throws Exception when an exception occurred
     */
    public static void shutdown() throws Exception {
        INSTANCE.shutdown();
    }
}
